# LibrarianAgent

基于 RAG（检索增强生成）的企业内部知识库问答系统，支持中英文双语、多轮对话与引用溯源。

---

## 技术栈

| 层级         | 技术选型                                      |
| ------------ | --------------------------------------------- |
| 后端框架     | Java 21 + Spring Boot 3.2                     |
| AI 编排      | Spring AI（ZhiPuAI Starter）                  |
| 生成模型     | GLM-4.6V（智谱 AI）                           |
| 向量数据库   | Milvus 2.4（HNSW 索引，1024 维 BGE-M3 嵌入）   |
| 缓存/会话    | Redis 7                                       |
| 元数据存储   | MySQL 8.0                                     |
| 文档解析     | Apache Tika + PDFBox                          |
| 重排序       | BGE-Reranker-v2-m3（独立服务）                |
| 可观测性     | Spring Boot Actuator + Micrometer + Prometheus|
| 容器化       | Docker Compose                                |

---

## 快速开始

### 前置要求

| 组件       | 版本       |
| ---------- | ---------- |
| JDK        | 21+        |
| Maven      | 3.9+       |
| Docker     | 24+        |
| Docker Compose | 2.20+  |

### 1. 启动依赖服务

```bash
cd docker
docker compose up -d
```

启动的服务包括：
- **Milvus** (端口 19530) — 向量存储
- **Redis** (端口 6379) — 会话缓存
- **MySQL** (端口 3306) — 元数据
- **Reranker** (端口 8001) — 重排序服务

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填入智谱 AI API Key
```

### 3. 构建并启动应用

```bash
# 方式一：Maven 直接运行
mvn spring-boot:run

# 方式二：构建 JAR 后运行
mvn package -DskipTests
java -jar target/librarian-agent-1.0.0-SNAPSHOT.jar

# 方式三：Docker 构建
docker compose -f docker/docker-compose.yml up --build
```

### 4. 验证服务

```bash
curl http://localhost:8080/api/actuator/health
```

返回 `{"status":"UP"}` 即表示服务正常。

---

## API 使用

### 问答接口

```bash
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "年假天数是如何规定的？",
    "topK": 5,
    "temperature": 0.1,
    "enableReranker": true
  }'
```

**响应示例：**

```json
{
  "traceId": "rag-a3f2b1c0",
  "answer": "根据《员工手册》第23页，正式员工根据工龄享有5-15天年假...",
  "citations": [
    {
      "source": "员工手册-2024版",
      "page": 23,
      "snippet": "正式员工享有年假5-15天，根据工龄...",
      "score": 0.89
    }
  ],
  "confidence": 0.89,
  "latencyMs": 3240,
  "rejected": false
}
```

### 多轮对话

首次请求不传 `sessionId`，系统自动创建并返回；后续请求携带该 `sessionId` 即可保持上下文：

```bash
# 第一轮
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "报销流程是什么？"}'

# 第二轮（携带 sessionId）
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"sessionId": "<上一步返回的sessionId>", "query": "需要哪些材料？"}'
```

### 文档上传

```bash
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -F "file=@员工手册.pdf" \
  -F "category=手册" \
  -F "language=zh"
```

### 评估

```bash
curl -X POST http://localhost:8080/api/v1/admin/evaluate \
  -H "Content-Type: application/json" \
  -d '{"evalSet": "default-125", "sampleSize": 50}'
```

---

## 项目结构

```
LibrarianAgent/
├── src/main/java/com/librarian/
│   ├── LibrarianAgentApplication.java   # 启动入口
│   ├── config/                          # 配置类
│   │   ├── MilvusConfig.java
│   │   ├── MilvusProperties.java
│   │   ├── RagProperties.java
│   │   ├── RedisConfig.java
│   │   ├── WebConfig.java
│   │   └── CorsConfig.java
│   ├── controller/                      # REST 控制器
│   │   ├── RagController.java
│   │   ├── DocumentController.java
│   │   ├── EvalController.java
│   │   └── SessionController.java
│   ├── model/
│   │   ├── dto/                         # 请求/响应 DTO
│   │   └── entity/                      # JPA 实体
│   ├── repository/                      # Spring Data JPA
│   ├── service/                         # 业务服务
│   │   ├── RagOrchestrationService.java # RAG 编排（核心）
│   │   ├── RagRetrievalService.java     # 检索 + 重排序
│   │   ├── RagGenerationService.java    # LLM 生成
│   │   ├── VectorStoreService.java      # Milvus 操作
│   │   ├── DocumentIngestService.java   # 文档摄入
│   │   ├── DocumentChunker.java         # 语义分块
│   │   ├── QueryRewriterService.java    # 多轮查询重写
│   │   ├── ConversationManager.java     # Redis 会话管理
│   │   └── EvalService.java            # 评估服务
│   ├── security/                        # 安全
│   │   ├── PiiMasker.java              # PII 脱敏
│   │   └── PromptInjectionGuard.java   # 提示注入防御
│   └── observability/                   # 可观测性
│       └── RagMetrics.java             # Micrometer 指标
├── src/test/java/com/librarian/         # 测试
├── src/main/resources/
│   └── application.yml                  # 应用配置
├── docker/
│   ├── docker-compose.yml              # 依赖服务编排
│   └── Dockerfile                       # 应用镜像
├── doc/                                 # 设计文档
└── pom.xml                              # Maven 依赖
```

---

## RAG 流水线

```
用户问题
  │
  ▼
Query Rewriter ──→ 独立检索查询（多轮对话指代消解）
  │
  ▼
Dense Retrieval ──→ Milvus ANN 检索（topK=10，BGE-M3 1024维）
  │
  ▼
Cross-Encoder Re-Ranker ──→ BGE-Reranker-v2-m3 精排（topN=5）
  │
  ▼
Context Assembler ──→ 截断至 4096 tokens + 来源标签
  │
  ▼
LLM Generation ──→ GLM-4.6V（temperature=0.1）
  │
  ▼
Response Validator ──→ 置信度评估 + 拒绝检测
  │
  ▼
带引用答案 + TraceLog 记录
```

---

## 关键指标（目标值）

| 指标             | 目标值  |
| ---------------- | ------- |
| 答案准确率       | ≥ 80%   |
| 忠实性           | ≥ 0.85  |
| 上下文精确度     | ≥ 0.70  |
| P90 端到端延迟   | < 10s   |
| 每1K调用成本     | ~¥0.98  |

---

## 开发指南

### 运行测试

```bash
mvn test
```

### 代码格式

使用 Spring Java Format：

```bash
mvn spring-javaformat:apply
```

### 调试

在 `application.yml` 中调整日志级别：

```yaml
logging:
  level:
    com.librarian: DEBUG
    org.springframework.ai: DEBUG
```

---

## 配置说明

| 配置项                           | 默认值       | 说明                     |
| -------------------------------- | ------------ | ------------------------ |
| `spring.ai.zhipuai.api-key`      | -            | 智谱 AI API Key          |
| `milvus.host`                    | localhost    | Milvus 服务地址          |
| `milvus.port`                    | 19530        | Milvus 服务端口          |
| `milvus.collection-name`         | rag_knowledge_base | 集合名          |
| `milvus.embedding-dimension`     | 1024         | 向量维度（BGE-M3）       |
| `milvus.search.top-k`            | 5            | 检索返回数量             |
| `rag.chunk.target-size`          | 512          | 分块目标大小（tokens）    |
| `rag.chunk.overlap`              | 128          | 分块重叠大小             |
| `rag.reranker.enabled`           | true         | 是否启用重排序           |
| `rag.similarity-threshold`       | 0.3          | 最低相关度阈值           |
| `rag.conversation.max-turns`     | 10           | 最大保留对话轮次         |
| `rag.conversation.redis-ttl`     | 3600         | 会话过期时间（秒）        |

---

## 许可证

MIT License
