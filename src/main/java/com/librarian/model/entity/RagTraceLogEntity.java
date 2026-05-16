package com.librarian.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rag_trace_logs")
public class RagTraceLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false)
    private String traceId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "query", columnDefinition = "TEXT")
    private String query;

    @Column(name = "rewritten_query", columnDefinition = "TEXT")
    private String rewrittenQuery;

    @Column(name = "retrieved_docs_json", columnDefinition = "TEXT")
    private String retrievedDocsJson;

    @Column(name = "generated_answer", columnDefinition = "TEXT")
    private String generatedAnswer;

    @Column(name = "citations_json", columnDefinition = "TEXT")
    private String citationsJson;

    @Column(name = "faithfulness_score")
    private Double faithfulnessScore;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "total_latency_ms")
    private Long totalLatencyMs;

    @Column(name = "retrieval_latency_ms")
    private Long retrievalLatencyMs;

    @Column(name = "generation_latency_ms")
    private Long generationLatencyMs;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "rejected")
    private Boolean rejected;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
