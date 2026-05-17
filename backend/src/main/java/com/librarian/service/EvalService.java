package com.librarian.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.librarian.mapper.DocumentRecordMapper;
import com.librarian.mapper.VectorDocumentChunkMapper;
import com.librarian.model.dto.EvalDto.*;
import com.librarian.model.entity.VectorDocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EvalService {

    @Autowired
    private DocumentRecordMapper documentRecordMapper;
    @Autowired
    private VectorDocumentChunkMapper vectorDocumentChunkMapper;
    @Autowired
    private QueryLogService queryLogService;

    public DashboardResponse getDashboard() {
        DocumentStats documentStats = buildDocumentStats();
        List<QueryLog> recentQueries = queryLogService.getRecentQueries();
        RetrievalMetrics retrievalMetrics = new RetrievalMetrics(
                queryLogService.getAvgSimilarity(),
                queryLogService.getAvgRetrievalTimeMs(),
                queryLogService.getAvgGenerationTimeMs(),
                queryLogService.getTotalQueries()
        );
        return new DashboardResponse(documentStats, recentQueries, retrievalMetrics);
    }

    private DocumentStats buildDocumentStats() {
        long total = 0, completed = 0, processing = 0, failed = 0;
        List<Map<String, Object>> statusCounts = documentRecordMapper.countByStatus();
        for (Map<String, Object> row : statusCounts) {
            String status = (String) row.get("status");
            long cnt = ((Number) row.get("cnt")).longValue();
            total += cnt;
            switch (status) {
                case "completed" -> completed = cnt;
                case "processing" -> processing = cnt;
                case "failed" -> failed = cnt;
            }
        }
        long totalChunks = vectorDocumentChunkMapper.selectCount(new LambdaQueryWrapper<VectorDocumentChunk>());
        return new DocumentStats(total, completed, processing, failed, totalChunks);
    }
}
