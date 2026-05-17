package com.librarian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.librarian.model.entity.DocumentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DocumentRecordMapper extends BaseMapper<DocumentRecord> {

    @Select("SELECT status, COUNT(*) AS cnt FROM document_record WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus();
}
