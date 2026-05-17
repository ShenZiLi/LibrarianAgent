package com.librarian.service.rag;

import com.librarian.config.RagProperties;
import com.librarian.model.entity.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class VectorSearch {

    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private RagProperties ragProperties;

    public List<DocumentChunk> search(String query) {
        return search(query, ragProperties.getTopK(), ragProperties.getSimilarityThreshold());
    }

    public List<DocumentChunk> search(String query, int topK) {
        return search(query, topK, ragProperties.getSimilarityThreshold());
    }

    public List<DocumentChunk> search(String query, int topK, double similarityThreshold) {
        log.info("Searching for: \"{}\" (topK={}, threshold={})", query, topK, similarityThreshold);

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
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
