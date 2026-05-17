package com.librarian.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.librarian.mapper.DocumentRecordMapper;
import com.librarian.mapper.VectorDocumentChunkMapper;
import com.librarian.model.dto.EvalDto.*;
import com.librarian.model.entity.DocumentRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
        long total = documentRecordMapper.selectCount(new LambdaQueryWrapper<>());
        long completed = documentRecordMapper.selectCount(
                new LambdaQueryWrapper<DocumentRecord>().eq(DocumentRecord::getStatus, "completed"));
        long processing = documentRecordMapper.selectCount(
                new LambdaQueryWrapper<DocumentRecord>().eq(DocumentRecord::getStatus, "processing"));
        long failed = documentRecordMapper.selectCount(
                new LambdaQueryWrapper<DocumentRecord>().eq(DocumentRecord::getStatus, "failed"));
        long totalChunks = vectorDocumentChunkMapper.selectCount(new LambdaQueryWrapper<>());
        return new DocumentStats(total, completed, processing, failed, totalChunks);
    }
}
