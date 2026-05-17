package com.librarian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.librarian.model.entity.DocumentRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentRecordMapper extends BaseMapper<DocumentRecord> {
}
