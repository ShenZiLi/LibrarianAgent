package com.librarian.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.librarian.model.dto.DocumentDto.DocumentResponse;
import com.librarian.service.IngestionService;
import com.librarian.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    @Autowired
    private IngestionService ingestionService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        LoggerUtil.setRequestId();
        log.info("Uploading document: {}", file.getOriginalFilename());
        try {
            byte[] fileContent = file.getBytes();
            ingestionService.ingestDocument(fileContent, file.getOriginalFilename(), file.getContentType(), file.getSize());
        } catch (Exception e) {
            log.error("Failed to read uploaded file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError().body("Failed to read uploaded file");
        }
        return ResponseEntity.accepted().body("Document upload accepted for processing");
    }

    @GetMapping
    public Page<DocumentResponse> listDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        LoggerUtil.setRequestId();
        log.info("Listing documents: page={}, size={}, status={}", page, size, status);
        return ingestionService.listDocuments(page, size, status);
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

    @PostMapping("/{documentId}/retry")
    public ResponseEntity<String> retryDocument(@PathVariable String documentId) {
        LoggerUtil.setRequestId();
        log.info("Retrying document: {}", documentId);
        ingestionService.retryDocument(documentId);
        return ResponseEntity.accepted().body("Document retry accepted for processing");
    }
}
