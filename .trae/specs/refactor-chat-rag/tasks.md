# Tasks

- [x] Task 1: 实现DocumentParser - PDF和Markdown/TXT解析
  - [x] Task 1.1: 实现PDF解析（Apache PDFBox提取文本）
  - [x] Task 1.2: 实现扫描PDF OCR识别（Tess4J）
  - [x] Task 1.3: 实现Markdown/TXT直接读取

- [x] Task 2: 实现TextChunker - 文本分块逻辑
  - [x] Task 2.1: 实现基于tokens的分块，支持chunk-size和overlap配置
  - [x] Task 2.2: 优先在段落/标题边界处分块

- [x] Task 3: 改造IngestionService - 实现文档导入管线
  - [x] Task 3.1: 串联DocumentParser → TextChunker → VectorStore
  - [x] Task 3.2: 使用Spring AI VectorStore存储文档向量
  - [x] Task 3.3: 携带文档元数据（fileName、章节等）

- [x] Task 4: 实现VectorSearch - ChromaDB向量检索
  - [x] Task 4.1: 使用Spring AI VectorStore进行相似度搜索
  - [x] Task 4.2: 支持top-k和相似度阈值过滤

- [x] Task 5: 实现ContextBuilder - 组装Prompt上下文
  - [x] Task 5.1: 将检索结果格式化为prompt上下文
  - [x] Task 5.2: 添加引用标记

- [x] Task 6: 实现QueryRewriter - 对话历史改写查询
  - [x] Task 6.1: 结合最近N轮对话历史，使用LLM改写查询为独立可检索的问题

- [x] Task 7: 实现LlmGenerator - GLM模型调用
  - [x] Task 7.1: 使用Spring AI ChatClient调用GLM模型
  - [x] Task 7.2: 实现同步生成方法
  - [x] Task 7.3: 实现Flux流式生成方法

- [x] Task 8: 改造ChatService - 接入RAG Pipeline
  - [x] Task 8.1: 注入RAG组件依赖
  - [x] Task 8.2: 改造sendMessage方法：改写查询→检索→组装→生成→返回
  - [x] Task 8.3: 改造streamMessage方法：改写查询→检索→组装→流式生成

- [x] Task 9: 完善配置 - 加载prompt模板
  - [x] Task 9.1: 配置Spring AI PromptTemplate加载rag-system-prompt.st
  - [x] Task 9.2: 在AiConfig中暴露prompt相关配置

# Task Dependencies

- Task 1 (DocumentParser) → Task 3 (IngestionService)
- Task 2 (TextChunker) → Task 3 (IngestionService)
- Task 3 (IngestionService) → Task 4 (VectorSearch)
- Task 4 (VectorSearch) → Task 5 (ContextBuilder) → Task 7 (LlmGenerator) → Task 8 (ChatService)
- Task 6 (QueryRewriter) → Task 8 (ChatService)
- Task 1, 2可并行
- Task 4, 5可并行（依赖Task 3完成前提）
- Task 7可独立于Task 4,5,6先行实现
