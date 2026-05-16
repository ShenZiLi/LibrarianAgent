# RAG对话链路改造 Spec

## Why
当前ChatController和ChatService的对话链路仅返回占位响应，RAG Pipeline组件全部为空实现。需要基于Spring AI框架接入GLM模型，实现完整的文档解析→分块→向量化→检索→生成对话链路。

## What Changes
- 实现RAG Pipeline所有组件的真实逻辑（DocumentParser、TextChunker、VectorSearch、ContextBuilder、QueryRewriter、LlmGenerator）
- 改造ChatService，将占位响应替换为真实的RAG对话流程
- 实现IngestionService的文档导入管线，使用Spring AI VectorStore存储
- 配置GLM模型调用，使用spring-ai-openai-spring-boot-starter兼容调用智谱GLM API
- 实现SSE流式输出
- **BREAKING**: ChatService.sendMessage和streamMessage方法签名不变，但行为从占位返回变为真实RAG调用

## Impact
- Affected specs: RAG问答能力、文档导入能力、流式输出能力
- Affected code:
  - `backend/src/main/java/com/librarian/service/ChatService.java`
  - `backend/src/main/java/com/librarian/service/IngestionService.java`
  - `backend/src/main/java/com/librarian/service/rag/` (全部6个组件)
  - `backend/src/main/resources/application.yml`
  - `backend/src/main/resources/prompts/rag-system-prompt.st`

## ADDED Requirements

### Requirement: 文档解析能力
系统 SHALL 支持PDF和Markdown/TXT格式的文档解析为纯文本。

#### Scenario: PDF文档解析成功
- **WHEN** 上传PDF文件
- **THEN** 使用Apache PDFBox提取文本内容
- **AND** 对扫描PDF使用Tess4J进行OCR识别

#### Scenario: Markdown/TXT文档解析
- **WHEN** 上传.md或.txt文件
- **THEN** 直接读取文本内容

### Requirement: 文档分块与向量化
系统 SHALL 将文档分割为适当大小的chunk并存储到ChromaDB向量数据库。

#### Scenario: 文档导入管线执行
- **WHEN** 文档解析完成
- **THEN** 按512 tokens分块，128 tokens重叠
- **AND** 调用EmbeddingModel生成向量
- **AND** 存储到ChromaDB并携带文档元数据

### Requirement: RAG问答能力
系统 SHALL 基于检索到的上下文生成答案，并附引用来源。

#### Scenario: 用户提问
- **WHEN** 用户发送消息
- **THEN** 结合对话历史改写查询
- **AND** 在ChromaDB中检索top-k相似文档
- **AND** 组装上下文调用GLM生成答案
- **AND** 答案附引用来源标签

#### Scenario: 检索结果为空或相似度低
- **WHEN** 检索结果为空或最高相似度低于阈值
- **THEN** 返回解释性拒绝答复

### Requirement: SSE流式输出
系统 SHALL 支持通过Server-Sent Events流式输出LLM生成内容。

#### Scenario: 流式问答
- **WHEN** 调用streamMessage端点
- **THEN** 返回SseEmitter
- **AND** 逐块发送生成的token
- **AND** 完成后发送done事件

## MODIFIED Requirements

### Requirement: 对话管理
ChatService的sendMessage和streamMessage方法 SHALL 调用RAG Pipeline而非返回占位文本。
