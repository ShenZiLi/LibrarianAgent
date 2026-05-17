package com.librarian.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class EvalDto {

    public record DocumentStats(
            long totalDocuments,
            long completedDocuments,
            long processingDocuments,
            long failedDocuments,
            long totalChunks
    ) {}

    public record QueryLog(
            String query,
            int retrievedDocs,
            double avgSimilarity,
            long retrievalTimeMs,
            long generationTimeMs,
            Instant timestamp
    ) {}

    public record RetrievalMetrics(
            double avgSimilarity,
            long avgRetrievalTimeMs,
            long avgGenerationTimeMs,
            int totalQueries
    ) {}

    public record DashboardResponse(
            DocumentStats documentStats,
            List<QueryLog> recentQueries,
            RetrievalMetrics retrievalMetrics
    ) {}
}
