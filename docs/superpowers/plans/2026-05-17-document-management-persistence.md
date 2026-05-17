# 文档管理持久化实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完善文档管理持久化功能，包括 chunk ID 记录实现精确向量删除、分页查询、失败重试、超时自动标记。

**Architecture:** 在现有 MySQL + MyBatis Plus 基础上，新增 document_chunk 表记录每个分块的向量 ID，实现精确删除替代不可靠的 similaritySearch 方案。IngestionService 重构为完整的文档生命周期管理，DocumentController 添加分页和重试接口。

**Tech Stack:** Spring Boot 3.4.1, Spring AI 1.1.4, MyBatis Plus 3.5.7, MySQL, ChromaDB

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `model/entity/DocumentRecord.java` | Modify | 新增 errorMessage 字段 |
| `model/entity/VectorDocumentChunk.java` | Create | 向量分块记录实体（MySQL表映射） |
| `mapper/VectorDocumentChunkMapper.java` | Create | 向量分块 Mapper |
| `mapper/DocumentRecordMapper.java` | No change | 已有 |
| `service/IngestionService.java` | Modify | chunk ID 记录、精确删除、重试、超时扫描 |
| `controller/DocumentController.java` | Modify | 分页查询、状态筛选、重试接口 |
| `model/dto/DocumentDto.java` | Modify | 新增 errorMessage 字段、分页响应 |
| `config/RagProperties.java` | Modify | 新增超时配置 |
| `LibrarianAgentApplication.java` | Modify | 添加 @MapperScan |
| `resources/schema.sql` | Modify | 新增 document_chunk 表、document_record 加字段 |
| `resources/application.yml` | Modify | 新增 rag 超时配置 |

---

### Task 1: 数据模型与建表脚本

**Files:**
- Modify: `backend/src/main/java/com/librarian/model/entity/DocumentRecord.java`
- Create: `backend/src/main/java/com/librarian/model/entity/VectorDocumentChunk.java`
- Create: `backend/src/main/java/com/librarian/mapper/VectorDocumentChunkMapper.java`
- Modify: `backend/src/main/resources/schema.sql`

- [ ] **Step 1: DocumentRecord 新增 errorMessage 字段**

在 `DocumentRecord.java` 的 `chunkCount` 字段后添加：

```java
private String errorMessage;
```

完整文件内容：

```java
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

    private String errorMessage;

    private Instant createdAt;

    private Instant updatedAt;

    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 2: 创建 VectorDocumentChunk 实体**

创建 `backend/src/main/java/com/librarian/model/entity/VectorDocumentChunk.java`：

```java
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
```

- [ ] **Step 3: 创建 VectorDocumentChunkMapper**

创建 `backend/src/main/java/com/librarian/mapper/VectorDocumentChunkMapper.java`：

```java
package com.librarian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.librarian.model.entity.VectorDocumentChunk;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VectorDocumentChunkMapper extends BaseMapper<VectorDocumentChunk> {
}
```

- [ ] **Step 4: 更新 schema.sql**

替换 `backend/src/main/resources/schema.sql` 全部内容：

```sql
CREATE DATABASE IF NOT EXISTS librarian DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE librarian;

CREATE TABLE IF NOT EXISTS document_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id   VARCHAR(64) NOT NULL,
    file_name     VARCHAR(512) NOT NULL,
    file_type     VARCHAR(128),
    file_size     BIGINT DEFAULT 0,
    status        VARCHAR(32) NOT NULL DEFAULT 'processing',
    chunk_count   INT DEFAULT 0,
    error_message VARCHAR(1024),
    created_at    DATETIME NOT NULL,
    updated_at    DATETIME NOT NULL,
    deleted       INT DEFAULT 0,
    UNIQUE KEY uk_document_id (document_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_chunk (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id  VARCHAR(64) NOT NULL,
    chunk_index  INT NOT NULL,
    chunk_text   TEXT,
    vector_id    VARCHAR(128) NOT NULL,
    created_at   DATETIME NOT NULL,
    INDEX idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/librarian/model/entity/DocumentRecord.java backend/src/main/java/com/librarian/model/entity/VectorDocumentChunk.java backend/src/main/java/com/librarian/mapper/VectorDocumentChunkMapper.java backend/src/main/resources/schema.sql
git commit -m "feat: add document_chunk table and errorMessage field for document persistence"
```

---

### Task 2: 配置更新

**Files:**
- Modify: `backend/src/main/java/com/librarian/LibrarianAgentApplication.java`
- Modify: `backend/src/main/java/com/librarian/config/RagProperties.java`
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: 启动类添加 @MapperScan**

修改 `LibrarianAgentApplication.java`：

```java
package com.librarian;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.librarian.mapper")
public class LibrarianAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibrarianAgentApplication.class, args);
    }
}
```

- [ ] **Step 2: RagProperties 新增超时配置**

修改 `RagProperties.java`，在 `sessionTimeoutMinutes` 字段后添加：

```java
private int processingTimeoutMinutes = 10;
private int processingCheckInterval = 300000;
```

完整文件内容：

```java
package com.librarian.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private int chunkSize = 512;
    private int chunkOverlap = 128;
    private int topK = 5;
    private double similarityThreshold = 0.7;
    private int maxHistoryRounds = 10;
    private int sessionTimeoutMinutes = 30;
    private int processingTimeoutMinutes = 10;
    private int processingCheckInterval = 300000;
}
```

- [ ] **Step 3: application.yml 新增 rag 超时配置**

在 `application.yml` 的 `rag:` 部分末尾添加：

```yaml
rag:
  chunk-size: 512
  chunk-overlap: 128
  top-k: 5
  similarity-threshold: 0.7
  max-history-rounds: 10
  session-timeout-minutes: 30
  processing-timeout-minutes: 10
  processing-check-interval: 300000
```

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/librarian/LibrarianAgentApplication.java backend/src/main/java/com/librarian/config/RagProperties.java backend/src/main/resources/application.yml
git commit -m "feat: add MapperScan, timeout config for document processing"
```

---

### Task 3: 重构 IngestionService

**Files:**
- Modify: `backend/src/main/java/com/librarian/service/IngestionService.java`

- [ ] **Step 1: 重构 IngestionService 完整实现**

替换 `IngestionService.java` 全部内容：

```java
package com.librarian.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.librarian.config.RagProperties;
import com.librarian.mapper.DocumentRecordMapper;
import com.librarian.mapper.VectorDocumentChunkMapper;
import com.librarian.model.dto.DocumentDto.DocumentResponse;
import com.librarian.model.entity.DocumentChunk;
import com.librarian.model.entity.DocumentRecord;
import com.librarian.model.entity.VectorDocumentChunk;
import com.librarian.service.rag.DocumentParser;
import com.librarian.service.rag.TextChunker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class IngestionService {

    private final DocumentRecordMapper documentRecordMapper;
    private final VectorDocumentChunkMapper vectorDocumentChunkMapper;
    private final DocumentParser documentParser;
    private final TextChunker textChunker;
    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public IngestionService(DocumentRecordMapper documentRecordMapper,
                            VectorDocumentChunkMapper vectorDocumentChunkMapper,
                            DocumentParser documentParser,
                            TextChunker textChunker,
                            VectorStore vectorStore,
                            RagProperties ragProperties) {
        this.documentRecordMapper = documentRecordMapper;
        this.vectorDocumentChunkMapper = vectorDocumentChunkMapper;
        this.documentParser = documentParser;
        this.textChunker = textChunker;
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
    }

    @Async("documentIngestionExecutor")
    public void ingestDocument(MultipartFile file) {
        String documentId = java.util.UUID.randomUUID().toString();
        log.info("Starting document ingestion: {} (id={})", file.getOriginalFilename(), documentId);

        DocumentRecord record = new DocumentRecord();
        record.setDocumentId(documentId);
        record.setFileName(file.getOriginalFilename());
        record.setFileType(file.getContentType());
        record.setFileSize(file.getSize());
        record.setStatus("processing");
        record.setChunkCount(0);
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        documentRecordMapper.insert(record);

        doIngest(record, file);
    }

    public void retryDocument(String documentId) {
        DocumentRecord record = documentRecordMapper.selectOne(
                new LambdaQueryWrapper<DocumentRecord>()
                        .eq(DocumentRecord::getDocumentId, documentId)
        );
        if (record == null) {
            throw new RuntimeException("Document not found: " + documentId);
        }
        if (!"failed".equals(record.getStatus())) {
            throw new RuntimeException("Only failed documents can be retried, current status: " + record.getStatus());
        }

        List<VectorDocumentChunk> existingChunks = vectorDocumentChunkMapper.selectList(
                new LambdaQueryWrapper<VectorDocumentChunk>()
                        .eq(VectorDocumentChunk::getDocumentId, documentId)
        );
        if (!existingChunks.isEmpty()) {
            try {
                vectorStore.delete(existingChunks.stream().map(VectorDocumentChunk::getVectorId).toList());
            } catch (Exception e) {
                log.warn("Failed to clean up existing vectors for retry {}: {}", documentId, e.getMessage());
            }
            vectorDocumentChunkMapper.delete(
                    new LambdaQueryWrapper<VectorDocumentChunk>()
                            .eq(VectorDocumentChunk::getDocumentId, documentId)
            );
        }

        record.setStatus("processing");
        record.setErrorMessage(null);
        record.setUpdatedAt(Instant.now());
        documentRecordMapper.updateById(record);

        doIngest(record, null);
    }

    private void doIngest(DocumentRecord record, MultipartFile file) {
        String documentId = record.getDocumentId();
        try {
            if (file == null) {
                throw new RuntimeException("File data not available for retry. Please re-upload the document.");
            }

            List<DocumentChunk> parsedChunks = documentParser.parse(file);
            if (parsedChunks.isEmpty()) {
                throw new RuntimeException("No content extracted from document");
            }

            List<DocumentChunk> chunkedDocuments = textChunker.chunkAll(parsedChunks);

            List<Document> aiDocuments = new ArrayList<>();
            for (int i = 0; i < chunkedDocuments.size(); i++) {
                DocumentChunk chunk = chunkedDocuments.get(i);
                Map<String, Object> metadata = Map.of(
                        "fileName", chunk.getMetadataAsString("fileName"),
                        "fileType", chunk.getMetadataAsString("fileType"),
                        "documentId", documentId
                );
                Document aiDoc = new Document(chunk.getContent(), metadata);
                aiDocuments.add(aiDoc);
            }

            vectorStore.add(aiDocuments);

            for (int i = 0; i < aiDocuments.size(); i++) {
                VectorDocumentChunk chunkRecord = new VectorDocumentChunk();
                chunkRecord.setDocumentId(documentId);
                chunkRecord.setChunkIndex(i);
                chunkRecord.setChunkText(aiDocuments.get(i).getText());
                chunkRecord.setVectorId(aiDocuments.get(i).getId());
                chunkRecord.setCreatedAt(Instant.now());
                vectorDocumentChunkMapper.insert(chunkRecord);
            }

            record.setStatus("completed");
            record.setChunkCount(aiDocuments.size());
            record.setUpdatedAt(Instant.now());
            documentRecordMapper.updateById(record);

            log.info("Document ingestion completed: {} ({} chunks)", documentId, aiDocuments.size());
        } catch (Exception e) {
            log.error("Document ingestion failed: {}", documentId, e);
            record.setStatus("failed");
            record.setErrorMessage(e.getMessage());
            record.setUpdatedAt(Instant.now());
            documentRecordMapper.updateById(record);
        }
    }

    public Page<DocumentResponse> listDocuments(int page, int size, String status) {
        LambdaQueryWrapper<DocumentRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(DocumentRecord::getStatus, status);
        }
        wrapper.orderByDesc(DocumentRecord::getCreatedAt);

        Page<DocumentRecord> recordPage = documentRecordMapper.selectPage(
                new Page<>(page, size), wrapper
        );

        Page<DocumentResponse> responsePage = new Page<>(
                recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal()
        );
        responsePage.setRecords(recordPage.getRecords().stream().map(this::toResponse).toList());
        return responsePage;
    }

    public DocumentResponse getDocument(String documentId) {
        DocumentRecord record = documentRecordMapper.selectOne(
                new LambdaQueryWrapper<DocumentRecord>()
                        .eq(DocumentRecord::getDocumentId, documentId)
        );
        if (record == null) {
            throw new RuntimeException("Document not found: " + documentId);
        }
        return toResponse(record);
    }

    public void deleteDocument(String documentId) {
        DocumentRecord record = documentRecordMapper.selectOne(
                new LambdaQueryWrapper<DocumentRecord>()
                        .eq(DocumentRecord::getDocumentId, documentId)
        );
        if (record == null) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        List<VectorDocumentChunk> chunks = vectorDocumentChunkMapper.selectList(
                new LambdaQueryWrapper<VectorDocumentChunk>()
                        .eq(VectorDocumentChunk::getDocumentId, documentId)
        );

        if (!chunks.isEmpty()) {
            try {
                vectorStore.delete(chunks.stream().map(VectorDocumentChunk::getVectorId).toList());
                log.info("Deleted {} vectors for document: {}", chunks.size(), documentId);
            } catch (Exception e) {
                log.warn("Failed to delete vectors for document {}: {}", documentId, e.getMessage());
            }
            vectorDocumentChunkMapper.delete(
                    new LambdaQueryWrapper<VectorDocumentChunk>()
                            .eq(VectorDocumentChunk::getDocumentId, documentId)
            );
        }

        documentRecordMapper.deleteById(record.getId());
        log.info("Document deleted: {}", documentId);
    }

    @Scheduled(fixedDelayString = "${rag.processing-check-interval:300000}")
    public void markTimeoutDocuments() {
        Instant timeoutThreshold = Instant.now().minusSeconds(ragProperties.getProcessingTimeoutMinutes() * 60L);

        List<DocumentRecord> timedOutRecords = documentRecordMapper.selectList(
                new LambdaQueryWrapper<DocumentRecord>()
                        .eq(DocumentRecord::getStatus, "processing")
                        .lt(DocumentRecord::getUpdatedAt, timeoutThreshold)
        );

        for (DocumentRecord record : timedOutRecords) {
            log.warn("Marking timed out document as failed: {}", record.getDocumentId());
            record.setStatus("failed");
            record.setErrorMessage("Processing timeout after " + ragProperties.getProcessingTimeoutMinutes() + " minutes");
            record.setUpdatedAt(Instant.now());
            documentRecordMapper.updateById(record);
        }
    }

    private DocumentResponse toResponse(DocumentRecord record) {
        return new DocumentResponse(
                record.getDocumentId(),
                record.getFileName(),
                record.getFileType(),
                record.getFileSize(),
                record.getStatus(),
                record.getChunkCount(),
                record.getErrorMessage(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/librarian/service/IngestionService.java
git commit -m "feat: refactor IngestionService with chunk ID tracking, precise delete, retry, timeout"
```

---

### Task 4: 更新 DTO 和 Controller

**Files:**
- Modify: `backend/src/main/java/com/librarian/model/dto/DocumentDto.java`
- Modify: `backend/src/main/java/com/librarian/controller/DocumentController.java`

- [ ] **Step 1: 更新 DocumentDto**

替换 `DocumentDto.java` 全部内容：

```java
package com.librarian.model.dto;

import java.time.Instant;

public class DocumentDto {

    public record DocumentResponse(
            String documentId,
            String fileName,
            String fileType,
            long fileSize,
            String status,
            int chunkCount,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
```

- [ ] **Step 2: 更新 DocumentController**

替换 `DocumentController.java` 全部内容：

```java
package com.librarian.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.librarian.model.dto.DocumentDto.DocumentResponse;
import com.librarian.service.IngestionService;
import com.librarian.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final IngestionService ingestionService;

    public DocumentController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        LoggerUtil.setRequestId();
        log.info("Uploading document: {}", file.getOriginalFilename());
        ingestionService.ingestDocument(file);
        return ResponseEntity.accepted().body("Document upload accepted for processing");
    }

    @GetMapping
    public Page<DocumentResponse> listDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        LoggerUtil.setRequestId();
        log.info("Listing documents: page={}, size={}, status={}", page, size, status);
        return ingestionService.listDocuments(page, size, status);
    }

    @GetMapping("/{documentId}")
    public DocumentResponse getDocument(@PathVariable String documentId) {
        LoggerUtil.setRequestId();
        log.info("Getting document: {}", documentId);
        return ingestionService.getDocument(documentId);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String documentId) {
        LoggerUtil.setRequestId();
        log.info("Deleting document: {}", documentId);
        ingestionService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{documentId}/retry")
    public ResponseEntity<String> retryDocument(@PathVariable String documentId) {
        LoggerUtil.setRequestId();
        log.info("Retrying document: {}", documentId);
        ingestionService.retryDocument(documentId);
        return ResponseEntity.accepted().body("Document retry accepted for processing");
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/librarian/model/dto/DocumentDto.java backend/src/main/java/com/librarian/controller/DocumentController.java
git commit -m "feat: add pagination, status filter, retry endpoint to DocumentController"
```

---

### Task 5: MyBatis Plus 分页插件配置

**Files:**
- Create: `backend/src/main/java/com/librarian/config/MyBatisPlusConfig.java`

- [ ] **Step 1: 创建 MyBatis Plus 分页插件配置**

创建 `backend/src/main/java/com/librarian/config/MyBatisPlusConfig.java`：

```java
package com.librarian.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/librarian/config/MyBatisPlusConfig.java
git commit -m "feat: add MyBatis Plus pagination plugin config"
```

---

### Task 6: 编译验证

**Files:** 无新增/修改

- [ ] **Step 1: 运行 Maven 编译**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 修复编译错误（如有）**

根据编译输出修复任何类型不匹配、缺失导入等问题。

- [ ] **Step 3: 提交修复（如有）**

```bash
git add -A && git commit -m "fix: resolve compilation errors"
```
