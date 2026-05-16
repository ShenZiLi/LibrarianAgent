package com.librarian.service.rag;

import com.librarian.config.AiConfig;
import com.librarian.model.entity.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class VectorSearch {

    private final VectorStore vectorStore;
    private final AiConfig aiConfig;

    public VectorSearch(VectorStore vectorStore, AiConfig aiConfig) {
        this.vectorStore = vectorStore;
        this.aiConfig = aiConfig;
    }

    public List<DocumentChunk> search(String query) {
        return search(query, aiConfig.getTopK());
    }

    public List<DocumentChunk> search(String query, int topK) {
        log.info("Searching for: \"{}\" (topK={})", query, topK);

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(aiConfig.getSimilarityThreshold())
                .build();

        List<Document> documents = vectorStore.similaritySearch(request);
        log.info("Found {} documents for query", documents.size());

        return documents.stream()
                .map(this::toDocumentChunk)
                .toList();
    }

    private DocumentChunk toDocumentChunk(Document doc) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(doc.getId());
        chunk.setContent(doc.getText());
        doc.getMetadata().forEach(chunk::addMetadata);
        return chunk;
    }
}
