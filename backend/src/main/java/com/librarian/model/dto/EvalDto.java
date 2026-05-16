package com.librarian.model.dto;

import java.util.Map;

public class EvalDto {

    public record EvalConfig(
            int topK,
            boolean enableReranker,
            double temperature,
            String testSetPath
    ) {}

    public record EvalResult(
            double faithfulness,
            double contextPrecision,
            double accuracy,
            Map<String, Object> metrics,
            java.time.Instant completedAt
    ) {}

    public record CostReport(
            long totalInputTokens,
            long totalOutputTokens,
            double estimatedCostPer1000Calls,
            Map<String, Double> sensitivityAnalysis
    ) {}
}
