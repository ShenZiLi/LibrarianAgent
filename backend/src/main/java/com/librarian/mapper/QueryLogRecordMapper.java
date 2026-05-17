package com.librarian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.librarian.model.entity.QueryLogRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QueryLogRecordMapper extends BaseMapper<QueryLogRecord> {

    @Select("SELECT COUNT(*) FROM query_log")
    long selectTotalCount();

    @Select("SELECT AVG(avg_similarity) FROM query_log")
    Double selectAvgSimilarity();

    @Select("SELECT AVG(retrieval_time_ms) FROM query_log")
    Double selectAvgRetrievalTimeMs();

    @Select("SELECT AVG(generation_time_ms) FROM query_log")
    Double selectAvgGenerationTimeMs();
}
