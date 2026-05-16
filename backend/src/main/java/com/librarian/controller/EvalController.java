package com.librarian.controller;

import com.librarian.model.dto.EvalDto.*;
import com.librarian.service.EvalService;
import com.librarian.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eval")
public class EvalController {

    private static final Logger log = LoggerFactory.getLogger(EvalController.class);

    private final EvalService evalService;

    public EvalController(EvalService evalService) {
        this.evalService = evalService;
    }

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
