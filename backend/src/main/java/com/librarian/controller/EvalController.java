package com.librarian.controller;

import com.librarian.model.dto.EvalDto.*;
import com.librarian.service.EvalService;
import com.librarian.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/eval")
public class EvalController {

    @Autowired
    private EvalService evalService;

    @PostMapping("/run")
    public String runEvaluation(@RequestBody(required = false) EvalConfig config) {
        LoggerUtil.setRequestId();
        log.info("Starting evaluation");
        evalService.runEvaluation(config);
        return "Evaluation started";
    }

    @GetMapping("/results")
    public EvalResult getResults() {
        LoggerUtil.setRequestId();
        log.info("Getting evaluation results");
        return evalService.getResults();
    }

    @GetMapping("/cost-estimate")
    public CostReport getCostEstimate(
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "false") boolean enableReranker,
            @RequestParam(defaultValue = "0.7") double temperature) {
        LoggerUtil.setRequestId();
        log.info("Getting cost estimate: topK={}, reranker={}, temp={}", 
                topK, enableReranker, temperature);
        return evalService.getCostEstimate(topK, enableReranker, temperature);
    }
}
