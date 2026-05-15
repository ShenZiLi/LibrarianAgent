package com.librarian.service;

import com.librarian.config.RagProperties;
import com.librarian.model.dto.RetrievedContext;
import io.milvus.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
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
        List<CreateCollectionReq.FieldSchema> fields = new ArrayList<>();

        fields.add(CreateCollectionReq.FieldSchema.builder()
                .name("id")
                .dataType(DataType.VarChar)
                .maxLength(64)
                .isPrimaryKey(true)
                .autoGenerate(false)
                .build());

        fields.add(CreateCollectionReq.FieldSchema.builder()
                .name("content")
                .dataType(DataType.VarChar)
                .maxLength(65535)
                .build());

        fields.add(CreateCollectionReq.FieldSchema.builder()
                .name("embedding")
                .dataType(DataType.FloatVector)
                .dimension(ragProperties.getEmbeddingDimension())
                .build());

        fields.add(CreateCollectionReq.FieldSchema.builder()
                .name("source_file")
                .dataType(DataType.VarChar)
                .maxLength(512)
                .build());

        fields.add(CreateCollectionReq.FieldSchema.builder()
                .name("page_number")
                .dataType(DataType.Int64)
                .build());

        fields.add(CreateCollectionReq.FieldSchema.builder()
                .name("section_title")
                .dataType(DataType.VarChar)
                .maxLength(512)
                .build());

        fields.add(CreateCollectionReq.FieldSchema.builder()
                .name("language")
                .dataType(DataType.VarChar)
                .maxLength(16)
                .build());

        fields.add(CreateCollectionReq.FieldSchema.builder()
                .name("document_type")
                .dataType(DataType.VarChar)
                .maxLength(64)
                .build());

        milvusClient.createCollection(CreateCollectionReq.builder()
                .collectionName(collectionName)
                .fieldSchemaList(fields)
                .enableDynamicField(true)
                .build());

        createIndex(collectionName);
    }

    private void createIndex(String collectionName) {
        IndexParam indexParam = IndexParam.builder()
                .fieldName("embedding")
                .indexType(IndexParam.IndexType.HNSW)
                .metricType(IndexParam.MetricType.COSINE)
                .extraParams(Map.of(
                        "M", ragProperties.getHnsw().getM(),
                        "efConstruction", ragProperties.getHnsw().getEfConstruction()
                ))
                .build();

        milvusClient.createIndex(io.milvus.v2.service.vector.request.IndexReq.builder()
                .collectionName(collectionName)
                .indexParams(List.of(indexParam))
                .build());
    }

    public void insertChunks(List<Map<String, Object>> chunkMaps) {
        if (chunkMaps.isEmpty()) {
            return;
        }

        try {
            InsertResp resp = milvusClient.insert(InsertReq.builder()
                    .collectionName(ragProperties.getCollectionName())
                    .data(chunkMaps)
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

        List<Double> queryEmbeddingList = embeddingModel.embed(query);

        List<Float> queryVector = queryEmbeddingList.stream()
                .map(Double::floatValue)
                .collect(Collectors.toList());

        SearchResp resp = milvusClient.search(SearchReq.builder()
                .collectionName(ragProperties.getCollectionName())
                .annsField("embedding")
                .data(List.of(queryVector))
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
