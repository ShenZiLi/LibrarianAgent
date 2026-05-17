package com.librarian.service;

import com.librarian.model.dto.EvalDto.QueryLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class QueryLogService {

    private static final int MAX_LOG_SIZE = 50;
    private final List<QueryLog> queryLogs = Collections.synchronizedList(new ArrayList<>());

    public void recordQuery(String query, int retrievedDocs, double avgSimilarity,
                            long retrievalTimeMs, long generationTimeMs) {
        QueryLog queryLog = new QueryLog(query, retrievedDocs, avgSimilarity,
                retrievalTimeMs, generationTimeMs, Instant.now());
        queryLogs.add(0, queryLog);
        if (queryLogs.size() > MAX_LOG_SIZE) {
            queryLogs.remove(queryLogs.size() - 1);
        }
    }

    public List<QueryLog> getRecentQueries() {
        return List.copyOf(queryLogs);
    }

    public double getAvgSimilarity() {
        if (queryLogs.isEmpty()) return 0.0;
        return queryLogs.stream()
                .mapToDouble(QueryLog::avgSimilarity)
                .average()
                .orElse(0.0);
    }

    public long getAvgRetrievalTimeMs() {
        if (queryLogs.isEmpty()) return 0;
        return (long) queryLogs.stream()
                .mapToLong(QueryLog::retrievalTimeMs)
                .average()
                .orElse(0.0);
    }

    public long getAvgGenerationTimeMs() {
        if (queryLogs.isEmpty()) return 0;
        return (long) queryLogs.stream()
                .mapToLong(QueryLog::generationTimeMs)
                .average()
                .orElse(0.0);
    }

    public int getTotalQueries() {
        return queryLogs.size();
    }
}
