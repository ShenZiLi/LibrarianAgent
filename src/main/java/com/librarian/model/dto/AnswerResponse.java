package com.librarian.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {

    private String traceId;

    private String answer;

    private List<Citation> citations;

    private Double confidence;

    private Long latencyMs;

    private Boolean rejected;

    private String rejectReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        private String source;
        private Integer page;
        private String snippet;
        private Double score;
    }

    public static AnswerResponse reject(String reason, String traceId) {
        return AnswerResponse.builder()
                .traceId(traceId)
                .answer("抱歉，我在知识库中没有找到相关信息。当前知识库涵盖：员工手册、合规指南、技术规格、架构文档。您可以尝试用不同的关键词重新提问。")
                .citations(List.of())
                .confidence(0.0)
                .rejected(true)
                .rejectReason(reason)
                .build();
    }
}
