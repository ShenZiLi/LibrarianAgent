package com.librarian.controller;

import com.librarian.model.dto.EvalRequest;
import com.librarian.model.dto.EvalResponse;
import com.librarian.service.EvalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class EvalController {

    private final EvalService evalService;

    @PostMapping("/evaluate")
    public ResponseEntity<EvalResponse> evaluate(@Valid @RequestBody EvalRequest request) {
        EvalResponse response = evalService.runEval(request.getEvalSet(), request.getSampleSize());
        return ResponseEntity.ok(response);
    }
}
