package com.librarian.controller;

import com.librarian.model.dto.DocumentUploadResponse;
import com.librarian.service.DocumentIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIngestService ingestService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "language", required = false, defaultValue = "auto") String language) throws IOException {

        DocumentUploadResponse response = ingestService.ingestDocument(file, category, language);
        return ResponseEntity.accepted().body(response);
    }
}
