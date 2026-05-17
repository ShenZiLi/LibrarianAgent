package com.librarian.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("query_log")
public class QueryLogRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String query;

    private Integer retrievedDocs;

    private Double avgSimilarity;

    private Long retrievalTimeMs;

    private Long generationTimeMs;

    private Instant createdAt;
}
