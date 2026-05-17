package com.librarian.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.librarian.mapper.QueryLogRecordMapper;
import com.librarian.model.dto.EvalDto.QueryLog;
import com.librarian.model.entity.QueryLogRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class QueryLogService {

    private static final int MAX_CACHE_SIZE = 50;

    private final List<QueryLog> recentCache = new CopyOnWriteArrayList<>();

    @Autowired
    private QueryLogRecordMapper queryLogRecordMapper;

    public void recordQuery(String query, int retrievedDocs, double avgSimilarity,
                            long retrievalTimeMs, long generationTimeMs) {
        QueryLogRecord record = new QueryLogRecord();
        record.setQuery(query);
        record.setRetrievedDocs(retrievedDocs);
        record.setAvgSimilarity(avgSimilarity);
        record.setRetrievalTimeMs(retrievalTimeMs);
        record.setGenerationTimeMs(generationTimeMs);
        record.setCreatedAt(Instant.now());
        queryLogRecordMapper.insert(record);

        QueryLog queryLog = new QueryLog(query, retrievedDocs, avgSimilarity,
                retrievalTimeMs, generationTimeMs, record.getCreatedAt());
        recentCache.add(0, queryLog);
        if (recentCache.size() > MAX_CACHE_SIZE) {
            recentCache.remove(recentCache.size() - 1);
        }
    }

    public List<QueryLog> getRecentQueries() {
        if (!recentCache.isEmpty()) {
            return List.copyOf(recentCache);
        }
        List<QueryLogRecord> records = queryLogRecordMapper.selectList(
                new LambdaQueryWrapper<QueryLogRecord>()
                        .orderByDesc(QueryLogRecord::getCreatedAt)
                        .last("LIMIT " + MAX_CACHE_SIZE));
        return records.stream().map(this::toQueryLog).toList();
    }

    public double getAvgSimilarity() {
        Double avg = queryLogRecordMapper.selectAvgSimilarity();
        return avg != null ? avg : 0.0;
    }

    public long getAvgRetrievalTimeMs() {
        Double avg = queryLogRecordMapper.selectAvgRetrievalTimeMs();
        return avg != null ? avg.longValue() : 0;
    }

    public long getAvgGenerationTimeMs() {
        Double avg = queryLogRecordMapper.selectAvgGenerationTimeMs();
        return avg != null ? avg.longValue() : 0;
    }

    public int getTotalQueries() {
        return (int) queryLogRecordMapper.selectTotalCount();
    }

    private QueryLog toQueryLog(QueryLogRecord record) {
        return new QueryLog(
                record.getQuery(),
                record.getRetrievedDocs(),
                record.getAvgSimilarity(),
                record.getRetrievalTimeMs(),
                record.getGenerationTimeMs(),
                record.getCreatedAt()
        );
    }
}
