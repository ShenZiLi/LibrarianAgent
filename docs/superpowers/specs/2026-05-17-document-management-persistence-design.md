# 文档管理持久化设计

## 背景

LibrarianAgent 的文档管理功能需要持久化记录已上传到 ChromaDB 向量数据库的文档信息。当前实现使用内存 ConcurrentHashMap，重启后数据丢失。已选择 MySQL + MyBatis Plus 作为持久化方案，基础框架已搭建完成，但存在以下问题需要解决：

1. **向量删除不可靠**：使用 similaritySearch + filterExpression 间接查找向量再删除，空查询字符串语义不明、topK 硬编码上限
2. **不支持分页查询**：listDocuments 返回全量数据
3. **缺少失败重试**：上传失败的文档只能删除后重新上传
4. **缺少超时处理**：processing 状态可能永久卡住

## 设计决策

| 决策项 | 选择 | 理由 |
|---|---|---|
| 向量删除方案 | MySQL 记录 chunk ID | 精确删除，无 topK 限制，无需 similaritySearch |
| 分页方案 | MyBatis Plus Page | 标准做法，支持 total/pages 等元数据 |
| 表初始化方式 | 手动执行建表脚本 | 用户偏好，避免自动执行的兼容性问题 |
| 额外功能 | 状态筛选 + 失败重试 + 超时自动标记 | 完善文档管理生命周期 |

## 数据模型

### document_record 表（已有，需微调）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT AUTO_INCREMENT | 主键 |
| document_id | VARCHAR(64) NOT NULL | 业务ID（UUID） |
| file_name | VARCHAR(512) NOT NULL | 文件名 |
| file_type | VARCHAR(128) | 文件类型 |
| file_size | BIGINT DEFAULT 0 | 文件大小 |
| status | VARCHAR(32) NOT NULL DEFAULT 'processing' | processing/completed/failed |
| chunk_count | INT DEFAULT 0 | 分块数量 |
| error_message | VARCHAR(1024) | 失败原因（新增） |
| created_at | DATETIME NOT NULL | 创建时间 |
| updated_at | DATETIME NOT NULL | 更新时间 |
| deleted | INT DEFAULT 0 | 逻辑删除标记 |

索引：`uk_document_id (document_id)`, `idx_status (status)`, `idx_created_at (created_at)`

### document_chunk 表（新增）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT AUTO_INCREMENT | 主键 |
| document_id | VARCHAR(64) NOT NULL | 关联 document_record.document_id |
| chunk_index | INT NOT NULL | 分块序号（从0开始） |
| chunk_text | TEXT | 分块文本内容（便于调试） |
| vector_id | VARCHAR(128) NOT NULL | Spring AI Document 在 ChromaDB 中的 ID |
| created_at | DATETIME NOT NULL | 创建时间 |

索引：`idx_document_id (document_id)`

## 核心服务逻辑

### ingestDocument 流程

1. 创建 DocumentRecord，status = processing
2. 解析文件 → 分块
3. 为每个 chunk 生成 Spring AI Document 时，记录 Document.getId() 到 document_chunk 表
4. 调用 vectorStore.add(documents) 写入 ChromaDB
5. 成功 → status = completed，更新 chunk_count
6. 失败 → status = failed，记录 error_message

### deleteDocument 流程

1. 查询 document_chunk 表获取该文档所有 vector_id
2. 调用 vectorStore.delete(vectorIds) 精确删除向量
3. 删除 document_chunk 表中对应记录
4. 逻辑删除 document_record

### retryDocument 流程

1. 校验 status = failed
2. 清理旧的 document_chunk 记录（如果有残留）
3. 重置 status = processing，清空 error_message
4. 重新执行解析 → 分块 → 写入 ChromaDB → 记录 chunk
5. 成功/失败更新状态

### 超时自动标记

- 使用 @Scheduled 定时任务，间隔由 rag.processing-check-interval 配置（默认 5 分钟）
- 查询 status = processing 且 updated_at 超过 rag.processing-timeout-minutes（默认 10 分钟）的记录
- 标记为 failed，error_message = "Processing timeout"

## REST API

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/v1/documents/upload | 上传文档（已有） |
| GET | /api/v1/documents?page=1&size=10&status= | 分页查询 + 状态筛选 |
| GET | /api/v1/documents/{documentId} | 查询单个文档（已有） |
| DELETE | /api/v1/documents/{documentId} | 删除文档（改进删除逻辑） |
| POST | /api/v1/documents/{documentId}/retry | 重试失败文档（新增） |

### 分页响应格式

```json
{
  "records": [...],
  "total": 42,
  "size": 10,
  "current": 1,
  "pages": 5
}
```

## 配置项

在 RagProperties 中新增：

| 属性 | 默认值 | 说明 |
|---|---|---|
| rag.processing-timeout-minutes | 10 | 处理超时阈值 |
| rag.processing-check-interval | 300000 | 定时检查间隔（毫秒） |

## 文件变更清单

| 文件 | 变更类型 | 说明 |
|---|---|---|
| DocumentRecord.java | 修改 | 新增 errorMessage 字段 |
| DocumentChunk.java | 新建 | chunk 记录实体 |
| DocumentChunkMapper.java | 新建 | chunk Mapper |
| IngestionService.java | 修改 | chunk ID 记录、精确删除、重试逻辑 |
| DocumentController.java | 修改 | 分页参数、状态筛选、重试接口 |
| RagProperties.java | 修改 | 新增超时配置 |
| schema.sql | 修改 | 新增 document_chunk 建表、document_record 加字段 |
| LibrarianAgentApplication.java | 修改 | 添加 @MapperScan |
