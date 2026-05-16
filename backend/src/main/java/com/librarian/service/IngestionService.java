package com.librarian.service;

import com.librarian.model.dto.DocumentDto.DocumentResponse;
import com.librarian.model.entity.DocumentChunk;
import com.librarian.service.rag.DocumentParser;
import com.librarian.service.rag.TextChunker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class IngestionService {

    private final Map<String, DocumentResponse> documents = new ConcurrentHashMap<>();

    private final DocumentParser documentParser;
    private final TextChunker textChunker;
    private final VectorStore vectorStore;

    public IngestionService(DocumentParser documentParser,
                            TextChunker textChunker,
                            VectorStore vectorStore) {
        this.documentParser = documentParser;
        this.textChunker = textChunker;
        this.vectorStore = vectorStore;
    }

    @Async("documentIngestionExecutor")
    public void ingestDocument(MultipartFile file) {
        String documentId = UUID.randomUUID().toString();
        log.info("Starting document ingestion: {} (id={})",
                file.getOriginalFilename(), documentId);

        DocumentResponse doc = new DocumentResponse(
                documentId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                "processing",
                0,
                Instant.now(),
                null
        );
        documents.put(documentId, doc);

        try {
            List<DocumentChunk> parsedChunks = documentParser.parse(file);
            if (parsedChunks.isEmpty()) {
                throw new RuntimeException("No content extracted from document");
            }

            List<DocumentChunk> chunkedDocuments = textChunker.chunkAll(parsedChunks);

            List<Document> aiDocuments = new ArrayList<>();
            for (DocumentChunk chunk : chunkedDocuments) {
                Map<String, Object> metadata = Map.of(
                        "fileName", chunk.getMetadataAsString("fileName"),
                        "fileType", chunk.getMetadataAsString("fileType"),
                        "documentId", documentId
                );
                aiDocuments.add(new Document(chunk.getContent(), metadata));
            }

            vectorStore.add(aiDocuments);

            DocumentResponse completed = new DocumentResponse(
                    documentId,
                    doc.fileName(),
                    doc.fileType(),
                    doc.fileSize(),
                    "completed",
                    aiDocuments.size(),
                    doc.createdAt(),
                    Instant.now()
            );
            documents.put(documentId, completed);
            log.info("Document ingestion completed: {} ({} chunks)", documentId, aiDocuments.size());
        } catch (Exception e) {
            log.error("Document ingestion failed: {}", documentId, e);
            DocumentResponse failed = new DocumentResponse(
                    documentId, doc.fileName(), doc.fileType(),
                    doc.fileSize(), "failed", 0, doc.createdAt(), null
            );
            documents.put(documentId, failed);
        }
    }

    public List<DocumentResponse> listDocuments() {
        return new ArrayList<>(documents.values());
    }

    public DocumentResponse getDocument(String documentId) {
        DocumentResponse doc = documents.get(documentId);
        if (doc == null) {
            throw new RuntimeException("Document not found: " + documentId);
        }
        return doc;
    }

    public void deleteDocument(String documentId) {
        documents.remove(documentId);
        log.info("Document deleted: {}", documentId);
    }
}
