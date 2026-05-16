package com.librarian.service;

import com.librarian.model.dto.DocumentDto.DocumentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final Map<String, DocumentResponse> documents = new ConcurrentHashMap<>();

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
            // TODO: Implement actual parsing, chunking, embedding pipeline
            Thread.sleep(1000);

            DocumentResponse completed = new DocumentResponse(
                    documentId,
                    doc.fileName(),
                    doc.fileType(),
                    doc.fileSize(),
                    "completed",
                    0,
                    doc.createdAt(),
                    Instant.now()
            );
            documents.put(documentId, completed);
            log.info("Document ingestion completed: {}", documentId);
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
