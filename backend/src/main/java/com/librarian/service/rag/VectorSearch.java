package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class VectorSearch {

    private final EmbeddingModel embeddingModel;

    @Value("${rag.top-k}")
    private int topK;

    @Value("${rag.similarity-threshold}")
    private double similarityThreshold;

    public VectorSearch(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<DocumentChunk> search(String query) {
        log.info("Searching for: {} (topK={})", query, topK);
        return Collections.emptyList();
    }

    public List<DocumentChunk> search(String query, int topK) {
        log.info("Searching for: {} (topK={})", query, topK);
        return Collections.emptyList();
    }
}
