package com.librarian.service;

import com.librarian.config.RagProperties;
import com.librarian.model.dto.RetrievedContext;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final MilvusClientV2 milvusClient;
    private final EmbeddingModel embeddingModel;
    private final RagProperties ragProperties;

    @PostConstruct
    public void initCollection() {
        String collectionName = ragProperties.getCollectionName();
        try {
            boolean exists = milvusClient.hasCollection(HasCollectionReq.builder()
                    .collectionName(collectionName)
                    .build());
            if (!exists) {
                createCollection(collectionName);
            }
            log.info("Milvus collection '{}' is ready", collectionName);
        } catch (Exception e) {
            log.error("Failed to initialize Milvus collection: {}", collectionName, e);
        }
    }

    private void createCollection(String collectionName) {
        CreateCollectionReq.CollectionSchema schema = milvusClient.createSchema();
        
        schema.addField(io.milvus.v2.service.collection.request.AddFieldReq.builder()
                .fieldName("id")
                .dataType(DataType.VarChar)
                .maxLength(64)
                .isPrimaryKey(true)
                .autoID(false)
                .build());

        schema.addField(io.milvus.v2.service.collection.request.AddFieldReq.builder()
                .fieldName("content")
                .dataType(DataType.VarChar)
                .maxLength(65535)
                .build());

        schema.addField(io.milvus.v2.service.collection.request.AddFieldReq.builder()
                .fieldName("embedding")
                .dataType(DataType.FloatVector)
                .dimension(ragProperties.getEmbeddingDimension())
                .build());

        schema.addField(io.milvus.v2.service.collection.request.AddFieldReq.builder()
                .fieldName("source_file")
                .dataType(DataType.VarChar)
                .maxLength(512)
                .build());

        schema.addField(io.milvus.v2.service.collection.request.AddFieldReq.builder()
                .fieldName("page_number")
                .dataType(DataType.Int64)
                .build());

        schema.addField(io.milvus.v2.service.collection.request.AddFieldReq.builder()
                .fieldName("section_title")
                .dataType(DataType.VarChar)
                .maxLength(512)
                .build());

        schema.addField(io.milvus.v2.service.collection.request.AddFieldReq.builder()
                .fieldName("language")
                .dataType(DataType.VarChar)
                .maxLength(16)
                .build());

        schema.addField(io.milvus.v2.service.collection.request.AddFieldReq.builder()
                .fieldName("document_type")
                .dataType(DataType.VarChar)
                .maxLength(64)
                .build());

        IndexParam indexParam = IndexParam.builder()
                .fieldName("embedding")
                .indexType(IndexParam.IndexType.HNSW)
                .metricType(IndexParam.MetricType.COSINE)
                .extraParams(Map.of(
                        "M", ragProperties.getHnsw().getM(),
                        "efConstruction", ragProperties.getHnsw().getEfConstruction()
                ))
                .build();

        milvusClient.createCollection(CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(schema)
                .indexParams(List.of(indexParam))
                .enableDynamicField(true)
                .build());
    }

    public void insertChunks(List<Map<String, Object>> chunkMaps) {
        if (chunkMaps.isEmpty()) {
            return;
        }

        try {
            List<com.google.gson.JsonObject> data = chunkMaps.stream().map(chunkMap -> {
                com.google.gson.JsonObject jsonObject = new com.google.gson.JsonObject();
                chunkMap.forEach((key, value) -> {
                    if (value instanceof List) {
                        com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
                        @SuppressWarnings("unchecked")
                        List<Float> floatList = (List<Float>) value;
                        floatList.forEach(jsonArray::add);
                        jsonObject.add(key, jsonArray);
                    } else if (value instanceof String) {
                        jsonObject.addProperty(key, (String) value);
                    } else if (value instanceof Number) {
                        jsonObject.addProperty(key, (Number) value);
                    }
                });
                return jsonObject;
            }).collect(Collectors.toList());

            InsertResp resp = milvusClient.insert(InsertReq.builder()
                    .collectionName(ragProperties.getCollectionName())
                    .data(data)
                    .build());

            log.info("Inserted {} chunks into Milvus, insert count: {}",
                    chunkMaps.size(), resp.getInsertCnt());
        } catch (Exception e) {
            log.error("Failed to insert chunks into Milvus", e);
            throw new RuntimeException("Failed to insert chunks into Milvus", e);
        }
    }

    public List<RetrievedContext> search(String query, int topK, Map<String, Object> filters) {
        long startTime = System.currentTimeMillis();

        float[] queryEmbedding = embeddingModel.embed(query);
        List<Float> queryVector = new ArrayList<>(queryEmbedding.length);
        for (float v : queryEmbedding) {
            queryVector.add(v);
        }

        SearchResp resp = milvusClient.search(SearchReq.builder()
                .collectionName(ragProperties.getCollectionName())
                .annsField("embedding")
                .data(List.of(new FloatVec(queryVector)))
                .topK(topK)
                .outputFields(List.of("id", "content", "source_file", "page_number",
                        "section_title", "language", "document_type"))
                .consistencyLevel(ConsistencyLevel.BOUNDED)
                .build());

        List<RetrievedContext> contexts = new ArrayList<>();
        List<List<SearchResp.SearchResult>> results = resp.getSearchResults();

        if (results != null && !results.isEmpty()) {
            for (SearchResp.SearchResult result : results.get(0)) {
                Map<String, Object> entity = result.getEntity();
                if (entity != null) {
                    RetrievedContext context = RetrievedContext.builder()
                            .chunkId(getStringValue(entity, "id"))
                            .content(getStringValue(entity, "content"))
                            .score((double) result.getScore())
                            .sourceFile(getStringValue(entity, "source_file"))
                            .pageNumber(getLongValue(entity, "page_number").intValue())
                            .sectionTitle(getStringValue(entity, "section_title"))
                            .language(getStringValue(entity, "language"))
                            .documentType(getStringValue(entity, "document_type"))
                            .build();
                    contexts.add(context);
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("Vector search completed in {}ms, returned {} results", elapsed, contexts.size());

        return contexts;
    }

    private String getStringValue(Map<String, Object> entity, String key) {
        Object value = entity.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLongValue(Map<String, Object> entity, String key) {
        Object value = entity.get(key);
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}
