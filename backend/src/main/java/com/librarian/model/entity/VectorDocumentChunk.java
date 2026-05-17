package com.librarian.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("document_chunk")
public class VectorDocumentChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String documentId;

    private Integer chunkIndex;

    private String chunkText;

    private String vectorId;

    private Instant createdAt;
}
