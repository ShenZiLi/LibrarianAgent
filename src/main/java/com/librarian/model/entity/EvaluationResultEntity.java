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
@Table(name = "evaluation_results")
public class EvaluationResultEntity {

    @Id
    @Column(name = "eval_id")
    private String evalId;

    @Column(name = "eval_set")
    private String evalSet;

    @Column(name = "sample_size")
    private Integer sampleSize;

    @Column(name = "accuracy")
    private Double accuracy;

    @Column(name = "faithfulness")
    private Double faithfulness;

    @Column(name = "context_precision")
    private Double contextPrecision;

    @Column(name = "avg_latency_ms")
    private Long avgLatencyMs;

    @Column(name = "p90_latency_ms")
    private Long p90LatencyMs;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
