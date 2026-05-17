package com.librarian.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("document_record")
public class DocumentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String documentId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String status;

    private Integer chunkCount;

    private Instant createdAt;

    private Instant updatedAt;

    @TableLogic
    private Integer deleted;
}
