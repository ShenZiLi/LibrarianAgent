package com.librarian.service;

import com.librarian.model.dto.EvalDto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EvalService {

    private EvalResult lastResult;

    public void runEvaluation(EvalConfig config) {
        log.info("Running evaluation with config: {}", config);
        // TODO: Implement actual evaluation logic
        lastResult = new EvalResult(
                0.0, 0.0, 0.0,
                Map.of("status", "not_implemented"),
                Instant.now()
        );
    }

    public EvalResult getResults() {
        if (lastResult == null) {
            return new EvalResult(
                    0.0, 0.0, 0.0,
                    Map.of("status", "no_eval_run"),
                    null
            );
        }
        return lastResult;
    }

    public CostReport getCostEstimate(int topK, boolean enableReranker, double temperature) {
        log.info("Estimating cost: topK={}, reranker={}, temp={}", topK, enableReranker, temperature);
        
        Map<String, Double> sensitivity = new HashMap<>();
        sensitivity.put("top_k_" + topK, 0.0);
        sensitivity.put("reranker_" + enableReranker, 0.0);
        sensitivity.put("temperature_" + temperature, 0.0);

        return new CostReport(
                0, 0, 0.0, sensitivity
        );
    }
}
