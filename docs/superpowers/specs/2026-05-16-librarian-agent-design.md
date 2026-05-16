# LibrarianAgent 技术设计文档

**日期**: 2026-05-16  
**版本**: v1.0  
**状态**: Draft - Awaiting Review

---

## 1. 概述

本文档描述 LibrarianAgent 系统的技术设计方案。该系统是一个基于 RAG（检索增强生成）的知识库问答系统，支持中英双语、多轮对话、引用溯源，满足 PRD 中定义的功能和非功能需求。

### 1.1 需求摘要

| 需求类型 | 要求 | 设计方案 |
|----------|------|----------|
| RAG问答 | 基于检索内容生成答案，附引用 | RAG Pipeline: 文档解析→分块→嵌入→检索→生成 |
| 多轮对话 | 同一会话保持上下文连续性 | ConversationManager 管理会话历史 |
| 延迟 | 90% 请求 < 10秒 | SSE流式输出 + 异步处理 + 超时控制 |
| 质量指标 | 忠实性 ≥ 0.85，上下文精确度 ≥ 0.70 | LLM评分 + 关键词重叠计算 |
| 日志追踪 | 支持监控和故障诊断 | 结构化日志 + 请求ID追踪 |
| 安全性 | 提示注入防御、PII处理 | 输入过滤 + PII脱敏 + 上下文严格约束 |

### 1.2 技术选型

| 层级 | 技术 | 选型理由 |
|------|------|----------|
| 后端框架 | Spring Boot 3.x + Spring AI | Java生态成熟，Spring AI提供RAG原生支持 |
| 前端框架 | Vue 3 + Vite | 轻量、TypeScript支持好、开发效率高 |
| UI组件库 | Element Plus | 企业级组件库，文档完善 |
| 向量数据库 | ChromaDB | 轻量级、本地文件存储、无需外部服务 |
| LLM服务 | 云端LLM API (GLM-4 / Qwen) | 性能好，通过Spring AI统一接口调用 |
| 文档解析 | Apache PDFBox + Tess4J | 支持PDF文本提取和OCR扫描识别 |
| 状态管理 | Pinia | Vue 3官方推荐，轻量 |
| 通信协议 | REST + SSE | SSE实现流式问答输出 |

---

## 2. 系统架构

### 2.1 整体架构图

```
┌──────────────────────────────────────────────────────────────┐
│                      Frontend (Vue 3 SPA)                    │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │  Chat UI   │  │ Document Mgmt│  │  Metrics/Eval Panel  │  │
│  │ (SSE流式)  │  │ (上传/状态)  │  │  (质量指标展示)      │  │
│  └─────┬──────┘  └──────┬───────┘  └──────────┬───────────┘  │
│        │                │                     │               │
└────────┼────────────────┼─────────────────────┼───────────────┘
         │ HTTP/REST + SSE                      │
         ▼                ▼                     ▼
┌──────────────────────────────────────────────────────────────┐
│                    Backend (Spring Boot)                      │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐   │
│  │ Chat API    │  │ Ingestion API│  │ Evaluation API     │   │
│  │ /chat/*     │  │ /docs/*      │  │ /eval/*            │   │
│  └──────┬──────┘  └──────┬───────┘  └──────────┬─────────┘   │
│         │                │                     │              │
│  ┌──────┴────────────────┴─────────────────────┴──────────┐  │
│  │                  RAG Pipeline Layer                     │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────┐ │  │
│  │  │Document  │  │Embedding │  │Vector    │  │LLM     │ │  │
│  │  │Parser    │→ │Service   │→ │Store     │→ │Generator│ │  │
│  │  │& Chunker │  │          │  │(ChromaDB)│  │+Citation│ │  │
│  │  └──────────┘  └──────────┘  └──────────┘  └────────┘ │  │
│  │  ┌──────────────────────────────────────────────────┐  │  │
│  │  │  Conversation Manager (会话上下文管理)           │  │  │
│  │  └──────────────────────────────────────────────────┘  │  │
│  │  ┌──────────────────────────────────────────────────┐  │  │
│  │  │  Logging & Observability (日志与可观测性)        │  │  │
│  │  └──────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────┐     ┌─────────────────────┐
│  Local ChromaDB     │     │  Cloud LLM API      │
│  (Vector Storage)   │     │  (GLM-4 / Qwen)     │
│  File-based         │     │  via Spring AI      │
└─────────────────────┘     └─────────────────────┘
```

### 2.2 数据流

**文档导入流程：**
```
用户上传 → DocumentController → IngestionService → DocumentParser → TextChunker → EmbeddingService → ChromaDB
```

**问答流程：**
```
用户提问 → ChatController → ChatService → QueryRewriter → VectorSearch → ContextBuilder → LlmGenerator → SSE Stream → 前端
```

---

## 3. 模块设计

### 3.1 文档处理模块 (Document Processing)

#### 3.1.1 组件职责

| 组件 | 职责 | 输入 | 输出 |
|------|------|------|------|
| DocumentParser | 解析不同格式文档为纯文本 | PDF/Markdown文件 | 文本内容 + 元数据 |
| TextChunker | 将长文本分割为合适大小的chunk | 长文本 | List<Chunk> |
| EmbeddingService | 将文本块转换为向量 | Chunk文本 | float[] 向量 |
| MetadataExtractor | 提取文档元数据 | 文档 | Metadata对象 |

#### 3.1.2 文档解析策略

- **PDF**: Apache PDFBox 提取文本内容
- **扫描PDF**: Tess4J (Tesseract Java绑定) 执行OCR识别
- **Markdown**: 直接读取，保留标题层级用于分块边界

#### 3.1.3 分块策略

- 分块大小: 512 tokens
- 重叠窗口: 128 tokens（保持上下文连贯性）
- 分块边界: 优先在段落/标题边界处分割
- 元数据保留: 每个chunk携带文档来源、章节、页码等信息

#### 3.1.4 异步处理

文档导入使用 `@Async` 注解，避免阻塞HTTP请求线程。导入状态通过API可查询。

### 3.2 RAG问答模块 (RAG Pipeline)

#### 3.2.1 组件职责

| 组件 | 职责 | 输入 | 输出 |
|------|------|------|------|
| QueryRewriter | 结合对话历史改写查询 | 用户问题 + 对话历史 | 独立可检索的查询 |
| VectorSearch | 在ChromaDB中检索相似文档 | 查询向量 | top-k相似chunk |
| ContextBuilder | 组装检索结果为prompt上下文 | 检索结果 | 格式化上下文 + 引用 |
| LlmGenerator | 调用LLM生成答案 | prompt + 上下文 | 流式答案 + 引用标注 |

#### 3.2.2 检索策略

- 检索模型: 向量相似度搜索 (cosine similarity)
- top_k: 默认 5，可通过配置调整
- 相似度阈值: 0.7，低于阈值触发拒绝答复
- 过滤: 可按文档类型、语言等元数据过滤

#### 3.2.3 拒绝策略

当检索未返回任何结果或最高相似度低于阈值时:
- 返回解释性答复，说明无法找到相关信息
- 清晰描述当前能力边界
- 建议用户重新表述问题

#### 3.2.4 Prompt 设计

系统提示词:
```
你是一个企业知识库助手。请基于以下检索到的上下文回答问题。

规则:
1. 答案必须严格基于提供的上下文
2. 如果上下文不足以回答问题，请明确说明
3. 在每个事实陈述后标注引用来源，格式如 [来源: 文档名, 章节]
4. 如果检测到个人身份信息，请进行脱敏处理

上下文:
{context}

问题: {question}
```

### 3.3 对话管理模块 (Conversation Manager)

#### 3.3.1 数据结构

```java
class ConversationSession {
    String sessionId;                          // UUID
    List<Message> history;                     // 最近N轮对话
    Map<String, Object> metadata;              // 扩展元数据
    Instant createdAt;
    Instant lastActiveAt;
}

class Message {
    String role;                               // "user" | "assistant"
    String content;
    Instant timestamp;
    List<String> citations;                    // 答案引用来源
}
```

#### 3.3.2 设计要点

- 存储: 内存 `ConcurrentHashMap<String, ConversationSession>`
- 过期机制: 30分钟不活跃自动清理（ScheduledTask）
- 历史窗口: 保留最近10轮对话用于上下文改写
- 并发安全: 使用线程安全集合

### 3.4 日志与可观测性模块 (Observability)

#### 3.4.1 日志层级

| 级别 | 用途 | 示例 |
|------|------|------|
| INFO | 关键业务事件 | 请求开始/结束、会话创建、文档导入完成 |
| DEBUG | 诊断信息 | 检索结果数量、相似度分数、chunk数量 |
| WARN | 异常但不阻塞 | 低相似度检索、LLM延迟高、PII检测 |
| ERROR | 系统错误 | 解析失败、API调用失败、系统异常 |

#### 3.4.2 日志格式

```json
{
  "timestamp": "2026-05-16T10:30:00Z",
  "level": "INFO",
  "requestId": "req-abc-123",
  "sessionId": "sess-xyz-456",
  "event": "chat_completed",
  "metrics": {
    "retrieval_time_ms": 245,
    "generation_time_ms": 3200,
    "total_time_ms": 3445,
    "retrieved_docs": 5,
    "avg_similarity": 0.82
  },
  "question_preview": "什么是公司的...",
  "answer_preview": "根据公司政策..."
}
```

#### 3.4.3 请求追踪

- 每个请求生成唯一 `requestId`
- 通过 MDC (Mapped Diagnostic Context) 在日志中传递
- 前端请求头携带 `X-Request-Id`

### 3.5 评估模块 (Evaluation)

#### 3.5.1 评估指标

| 指标 | 计算方法 | 目标阈值 |
|------|----------|----------|
| 忠实性 (Faithfulness) | LLM评分答案是否基于检索上下文 (0-1) | ≥ 0.85 |
| 上下文精确度 (Context Precision) | 检索的相关文档占比 | ≥ 0.70 |
| 答案准确率 | 预定义测试集人工评分匹配度 | ≥ 80% |

#### 3.5.2 成本估算

- 记录每次调用的 token 用量 (输入/输出)
- 提供敏感性分析接口:
  - 不同 top_k 值对成本和延迟的影响
  - reranker 开关对比
  - 不同温度值对比 (≥ 3档: 0.3, 0.7, 1.0)
- 输出每 1,000 次调用的估算成本

#### 3.5.3 PII 脱敏

检测并脱敏以下个人信息:
- 姓名 (替换为 `[姓名]`)
- 邮箱 (替换为 `[邮箱]`)
- 电话号码 (替换为 `[电话]`)
- 身份证号 (替换为 `[身份证号]`)

---

## 4. API 设计

### 4.1 文档管理

| 方法 | 路径 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | /api/v1/documents/upload | 上传文档 | multipart/form-data | DocumentStatus |
| GET | /api/v1/documents | 列出文档 | - | List<Document> |
| GET | /api/v1/documents/{id} | 获取文档详情 | - | Document |
| DELETE | /api/v1/documents/{id} | 删除文档 | - | - |

### 4.2 问答接口

| 方法 | 路径 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | /api/v1/chat/sessions | 创建会话 | CreateSessionRequest | Session |
| GET | /api/v1/chat/sessions | 列出会话 | - | List<Session> |
| GET | /api/v1/chat/sessions/{id} | 获取会话历史 | - | SessionWithMessages |
| POST | /api/v1/chat/sessions/{id}/message | 发送消息(同步) | MessageRequest | MessageResponse |
| POST | /api/v1/chat/sessions/{id}/stream | 发送消息(SSE) | MessageRequest | SSE Stream |

### 4.3 评估接口

| 方法 | 路径 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | /api/v1/eval/run | 运行评估 | EvalConfig | EvalJob |
| GET | /api/v1/eval/results | 获取评估结果 | - | EvalResult |
| GET | /api/v1/eval/cost-estimate | 成本估算 | CostParams | CostReport |

---

## 5. 前端设计

### 5.1 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 (Composition API) | ^3.4 |
| 构建工具 | Vite | ^5.0 |
| UI组件 | Element Plus | ^2.5 |
| 状态管理 | Pinia | ^2.1 |
| HTTP客户端 | Axios | ^1.6 |
| 路由 | Vue Router | ^4.2 |
| Markdown渲染 | markdown-it | ^14.0 |

### 5.2 页面结构

```
┌─────────────────────────────────────────┐
│  Header: LibrarianAgent                 │
├──────────────┬──────────────────────────┤
│ Sidebar      │  Main Content            │
│              │                          │
│ ● 新对话     │  ┌────────────────────┐  │
│              │  │  对话消息列表       │  │
│ 历史会话:     │  │                    │  │
│ ● 会话1      │  │  User: 问题...      │  │
│ ● 会话2      │  │  AI:  答案...       │  │
│              │  │  [引用: Doc1, p.3]  │  │
│ ---          │  └────────────────────┘  │
│ ● 文档管理   │  ┌────────────────────┐  │
│ ● 评估面板   │  │  输入框 [发送]      │  │
│              │  └────────────────────┘  │
└──────────────┴──────────────────────────┘
```

### 5.3 核心组件

| 组件 | 路径 | 功能 |
|------|------|------|
| ChatWindow | src/components/ChatWindow.vue | 聊天主窗口，管理消息流 |
| MessageItem | src/components/MessageItem.vue | 单条消息渲染，支持Markdown和引用 |
| DocumentUploader | src/components/DocumentUploader.vue | 拖拽上传、进度显示 |
| EvalPanel | src/components/EvalPanel.vue | 评估控制面板 |
| HomeView | src/views/HomeView.vue | 聊天主页 |
| DocumentView | src/views/DocumentView.vue | 文档管理页 |
| EvalView | src/views/EvalView.vue | 评估页 |

### 5.4 SSE 流式输出实现

```javascript
// 使用 EventSource API 接收流式响应
const eventSource = new EventSource(`/api/v1/chat/sessions/${sessionId}/stream?question=${encodedQuestion}`);

eventSource.onmessage = (event) => {
  // 追加新内容到消息缓冲区
  messageContent += event.data;
  // 实时更新UI
  updateMessageDisplay(messageContent);
};

eventSource.addEventListener('citations', (event) => {
  // 接收引用信息
  citations = JSON.parse(event.data);
});

eventSource.addEventListener('done', () => {
  eventSource.close();
});
```

---

## 6. 项目结构

```
LibrarianAgent/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/librarian/
│   │   ├── config/
│   │   │   ├── AiConfig.java         # Spring AI配置
│   │   │   ├── ChromaConfig.java     # ChromaDB配置
│   │   │   └── AsyncConfig.java      # 异步任务配置
│   │   ├── controller/
│   │   │   ├── ChatController.java   # 问答API
│   │   │   ├── DocumentController.java
│   │   │   └── EvalController.java
│   │   ├── service/
│   │   │   ├── rag/
│   │   │   │   ├── DocumentParser.java
│   │   │   │   ├── TextChunker.java
│   │   │   │   ├── VectorSearch.java
│   │   │   │   ├── ContextBuilder.java
│   │   │   │   ├── QueryRewriter.java
│   │   │   │   └── LlmGenerator.java
│   │   │   ├── ChatService.java
│   │   │   ├── IngestionService.java
│   │   │   └── EvalService.java
│   │   ├── model/
│   │   │   ├── dto/
│   │   │   └── entity/
│   │   └── util/
│   │       ├── LoggerUtil.java
│   │       └── PiiSanitizer.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── prompts/
│   │       └── rag-system-prompt.st
│   └── pom.xml
│
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── views/
│   │   ├── stores/
│   │   └── router/
│   ├── package.json
│   └── vite.config.ts
│
└── docs/
    └── PRD.md
```

---

## 7. 配置管理

### 7.1 application.yml 关键配置

```yaml
spring:
  ai:
    openai:
      base-url: ${LLM_BASE_URL:https://api.openai.com}
      api-key: ${LLM_API_KEY}
      chat:
        options:
          model: ${LLM_MODEL:glm-4}
          temperature: ${LLM_TEMPERATURE:0.7}
      embedding:
        options:
          model: ${EMBEDDING_MODEL:text-embedding-3-small}

rag:
  chunk-size: 512
  chunk-overlap: 128
  top-k: 5
  similarity-threshold: 0.7
  max-history-rounds: 10

server:
  port: ${SERVER_PORT:8080}
```

### 7.2 环境变量

| 变量 | 描述 | 默认值 |
|------|------|--------|
| LLM_BASE_URL | LLM API地址 | https://api.openai.com |
| LLM_API_KEY | API密钥 | (必填) |
| LLM_MODEL | 模型名称 | glm-4 |
| LLM_TEMPERATURE | 生成温度 | 0.7 |
| EMBEDDING_MODEL | 嵌入模型 | text-embedding-3-small |
| SERVER_PORT | 服务端口 | 8080 |

---

## 8. 延迟与成本优化

### 8.1 延迟优化

| 策略 | 描述 | 预期效果 |
|------|------|----------|
| SSE流式输出 | 立即开始输出，无需等待完整生成 | 用户感知延迟 < 2秒 |
| 批量嵌入 | 文档导入时批量调用Embedding API | 减少API调用次数 |
| ChromaDB超时 | 查询超时3秒 | 防止慢查询拖慢整体 |
| LLM超时 | 生成超时8秒 | 确保90%请求 < 10秒 |
| 连接池 | HTTP连接复用 | 减少连接建立开销 |

### 8.2 成本敏感性分析

| 参数 | 档位1 | 档位2 | 档位3 |
|------|-------|-------|-------|
| top_k | 3 (低) | 5 (中) | 10 (高) |
| reranker | 关 | - | 开 |
| temperature | 0.3 (保守) | 0.7 (平衡) | 1.0 (创意) |

---

## 9. 安全设计

### 9.1 提示注入防御

- 系统提示词严格限定回答范围
- 用户输入经过过滤和转义
- 检索上下文作为唯一知识来源

### 9.2 PII 处理

- 日志中的用户问题脱敏
- 答案中的个人信息替换
- 输入输出双向检测

### 9.3 API 安全

- CORS 配置限制跨域
- 请求频率限制 (可选)
- 敏感配置通过环境变量注入

---

## 10. 测试策略

### 10.1 单元测试

- DocumentParser: 各格式解析正确性
- TextChunker: 分块大小和边界
- PiiSanitizer: 各类PII检测
- VectorSearch: 检索准确性

### 10.2 集成测试

- RAG Pipeline端到端: 文档导入→问答
- API接口测试: 各端点正确性
- 多轮对话连续性: 上下文保持

### 10.3 评估测试

- 预定义测试集: 至少20个问答对
- 自动评分: 忠实性、上下文精确度、准确率
- 成本统计: token用量和估算
