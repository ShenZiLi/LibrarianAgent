package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class RagVectorStoreTest {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private VectorSearch vectorSearch;

    @Test
    void contextLoads() {
        assertNotNull(vectorStore, "VectorStore should be injected");
        assertNotNull(vectorSearch, "VectorSearch should be injected");
        log.info("Spring context loaded successfully, VectorStore and VectorSearch are available");
    }

    @Test
    void shouldAddAndSearchDocument() {
        String testContent = "这是一个RAG向量数据库测试文档，用于验证ChromaDB的写入和检索功能。";
        String testDocId = "test-doc-" + System.currentTimeMillis();

        Document testDoc = new Document(testContent, Map.of(
                "fileName", "test.txt",
                "fileType", "text",
                "documentId", testDocId
        ));

        vectorStore.add(List.of(testDoc));
        log.info("Added test document to vector store: id={}", testDoc.getId());

        SearchRequest request = SearchRequest.builder()
                .query("RAG向量数据库测试")
                .topK(3)
                .similarityThreshold(0.0)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        log.info("Search returned {} results", results.size());

        assertFalse(results.isEmpty(), "Should find at least one result for the test query");
        boolean found = results.stream().anyMatch(doc -> doc.getText().contains("RAG向量数据库测试"));
        assertTrue(found, "Search results should contain the test document");
        log.info("Test document found in search results");

        vectorStore.delete(List.of(testDoc.getId()));
        log.info("Cleaned up test document: id={}", testDoc.getId());
    }

    @Test
    void shouldSearchExistingData() {
        SearchRequest request = SearchRequest.builder()
                .query("知识库")
                .topK(5)
                .similarityThreshold(0.0)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        log.info("Search '知识库' returned {} results from existing data", results.size());

        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            log.info("  Result {}: id={}, text={}..., metadata={}",
                    i + 1,
                    doc.getId(),
                    doc.getText().substring(0, Math.min(50, doc.getText().length())),
                    doc.getMetadata());
        }
    }

    @Test
    void shouldSearchViaVectorSearchComponent() {
        List<DocumentChunk> results = vectorSearch.search("测试查询", 3);
        log.info("VectorSearch.search() returned {} results", results.size());

        for (int i = 0; i < results.size(); i++) {
            DocumentChunk chunk = results.get(i);
            log.info("  Chunk {}: id={}, content={}...",
                    i + 1,
                    chunk.getDocumentId(),
                    chunk.getContent().substring(0, Math.min(50, chunk.getContent().length())));
        }
    }
}
