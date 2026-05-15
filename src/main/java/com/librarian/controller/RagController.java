package com.librarian.controller;

import com.librarian.model.dto.AnswerResponse;
import com.librarian.model.dto.QueryRequest;
import com.librarian.service.RagOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagOrchestrationService orchestrationService;

    @PostMapping("/query")
    public ResponseEntity<AnswerResponse> query(@Valid @RequestBody QueryRequest request) {
        AnswerResponse response = orchestrationService.processQuery(request);
        return ResponseEntity.ok(response);
    }
}
