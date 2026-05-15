package com.librarian.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalResponse {

    private Double accuracy;

    private Double faithfulness;

    private Double contextPrecision;

    private Long avgLatencyMs;

    private Long p90LatencyMs;

    private List<EvalDetail> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvalDetail {
        private String question;
        private String expectedAnswer;
        private String actualAnswer;
        private Double faithfulnessScore;
        private Boolean isCorrect;
    }
}
