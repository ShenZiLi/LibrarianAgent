package com.librarian.controller;

import com.librarian.model.dto.DocumentDto.DocumentResponse;
import com.librarian.service.IngestionService;
import com.librarian.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final IngestionService ingestionService;

    public DocumentController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        LoggerUtil.setRequestId();
        log.info("Uploading document: {}", file.getOriginalFilename());
        ingestionService.ingestDocument(file);
        return ResponseEntity.accepted().body("Document upload accepted for processing");
    }

    @GetMapping
    public List<DocumentResponse> listDocuments() {
        LoggerUtil.setRequestId();
        log.info("Listing all documents");
        return ingestionService.listDocuments();
    }

    @GetMapping("/{documentId}")
    public DocumentResponse getDocument(@PathVariable String documentId) {
        LoggerUtil.setRequestId();
        log.info("Getting document: {}", documentId);
        return ingestionService.getDocument(documentId);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String documentId) {
        LoggerUtil.setRequestId();
        log.info("Deleting document: {}", documentId);
        ingestionService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
