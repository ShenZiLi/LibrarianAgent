# LibrarianAgent Backend Scaffolding Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the Maven-managed Spring Boot backend project structure with all configuration, base classes, and empty service stubs ready for RAG implementation.

**Architecture:** Single Spring Boot 3.x application with Spring AI integration, organized by feature (config, controller, service/rag, model, util). Maven multi-module ready with single module for MVP.

**Tech Stack:** Java 17, Spring Boot 3.2.x, Spring AI 1.0.0-M4, Maven, Apache PDFBox, Tess4J, Lombok, SLF4J

---

## File Map

All files under `backend/src/main/java/com/librarian/`:

| File | Responsibility |
|------|----------------|
| `config/AiConfig.java` | Spring AI ChatClient, EmbeddingModel bean configuration |
| `config/ChromaConfig.java` | ChromaDB client connection setup |
| `config/AsyncConfig.java` | @EnableAsync, thread pool configuration |
| `config/WebMvcConfig.java` | CORS configuration for frontend |
| `controller/ChatController.java` | REST endpoints for chat sessions and messages |
| `controller/DocumentController.java` | REST endpoints for document upload/management |
| `controller/EvalController.java` | REST endpoints for evaluation and cost estimation |
| `service/ChatService.java` | Session management, conversation orchestration |
| `service/IngestionService.java` | Document parsing, chunking, embedding pipeline |
| `service/EvalService.java` | Evaluation metrics, cost calculation |
| `service/rag/DocumentParser.java` | Parse PDF/Markdown to text |
| `service/rag/TextChunker.java` | Split text into chunks with overlap |
| `service/rag/VectorSearch.java` | Query ChromaDB for similar documents |
| `service/rag/ContextBuilder.java` | Format search results into prompt context |
| `service/rag/QueryRewriter.java` | Rewrite query using conversation history |
| `service/rag/LlmGenerator.java` | Generate answer from LLM with citations |
| `model/dto/ChatDto.java` | Chat request/response DTOs |
| `model/dto/DocumentDto.java` | Document request/response DTOs |
| `model/dto/EvalDto.java` | Evaluation request/response DTOs |
| `model/entity/ConversationSession.java` | In-memory session entity |
| `model/entity/Message.java` | Chat message entity |
| `model/entity/DocumentChunk.java` | Parsed document chunk entity |
| `util/PiiSanitizer.java` | PII detection and masking |
| `util/LoggerUtil.java` | Structured logging helpers |
| `LibrarianAgentApplication.java` | Spring Boot main class |

Resources:
- `backend/src/main/resources/application.yml` - Application configuration
- `backend/src/main/resources/prompts/rag-system-prompt.st` - RAG system prompt template

---

### Task 1: Maven pom.xml and Application Entry Point

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/librarian/LibrarianAgentApplication.java`

- [ ] **Step 1: Create pom.xml with all required dependencies**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.librarian</groupId>
    <artifactId>librarian-agent</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>LibrarianAgent</name>
    <description>RAG-based Knowledge Base Q&A System</description>

    <properties>
        <java.version>17</java.version>
        <spring-ai.version>1.0.0-M4</spring-ai.version>
        <pdfbox.version>3.0.2</pdfbox.version>
        <tess4j.version>5.10.0</tess4j.version>
        <chroma-client.version>0.3.0</chroma-client.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <!-- Spring AI -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
            <version>${spring-ai.version}</version>
        </dependency>

        <!-- PDF Processing -->
        <dependency>
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox</artifactId>
            <version>${pdfbox.version}</version>
        </dependency>

        <!-- OCR for scanned PDFs -->
        <dependency>
            <groupId>net.sourceforge.tess4j</groupId>
            <artifactId>tess4j</artifactId>
            <version>${tess4j.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create main application class**

Create `backend/src/main/java/com/librarian/LibrarianAgentApplication.java`:

```java
package com.librarian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LibrarianAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibrarianAgentApplication.class, args);
    }
}
```

- [ ] **Step 3: Verify Maven can resolve dependencies**

Run: `cd backend && mvn dependency:resolve`
Expected: BUILD SUCCESS with all dependencies downloaded

---

### Task 2: Configuration Classes

**Files:**
- Create: `backend/src/main/java/com/librarian/config/AiConfig.java`
- Create: `backend/src/main/java/com/librarian/config/ChromaConfig.java`
- Create: `backend/src/main/java/com/librarian/config/AsyncConfig.java`
- Create: `backend/src/main/java/com/librarian/config/WebMvcConfig.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Create application.yml configuration**

Create `backend/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: librarian-agent
  ai:
    openai:
      base-url: ${LLM_BASE_URL:https://open.bigmodel.cn/api/paas/v4/}
      api-key: ${LLM_API_KEY:your-api-key-here}
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
  session-timeout-minutes: 30

chroma:
  host: ${CHROMA_HOST:localhost}
  port: ${CHROMA_PORT:8000}
  collection-name: librarian-docs

server:
  port: ${SERVER_PORT:8080}

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - requestId=%X{requestId} - %msg%n"
  level:
    com.librarian: DEBUG
    org.springframework.ai: INFO
```

- [ ] **Step 2: Create AiConfig.java**

Create `backend/src/main/java/com/librarian/config/AiConfig.java`:

```java
package com.librarian.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${rag.top-k}")
    private int topK;

    @Value("${rag.similarity-threshold}")
    private double similarityThreshold;

    @Value("${rag.max-history-rounds}")
    private int maxHistoryRounds;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    public int getTopK() {
        return topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public int getMaxHistoryRounds() {
        return maxHistoryRounds;
    }
}
```

- [ ] **Step 3: Create AsyncConfig.java**

Create `backend/src/main/java/com/librarian/config/AsyncConfig.java`:

```java
package com.librarian.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "documentIngestionExecutor")
    public Executor documentIngestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("doc-ingestion-");
        executor.setRejectedExecutionHandler((r, e) -> 
            log.warn("Document ingestion queue is full, rejecting task"));
        executor.initialize();
        return executor;
    }
}
```

- [ ] **Step 4: Create WebMvcConfig.java**

Create `backend/src/main/java/com/librarian/config/WebMvcConfig.java`:

```java
package com.librarian.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

- [ ] **Step 5: Create ChromaConfig.java (stub - will be implemented when ChromaDB client is added)**

Create `backend/src/main/java/com/librarian/config/ChromaConfig.java`:

```java
package com.librarian.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChromaConfig {

    @Value("${chroma.host}")
    private String host;

    @Value("${chroma.port}")
    private int port;

    @Value("${chroma.collection-name}")
    private String collectionName;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getCollectionName() {
        return collectionName;
    }
}
```

- [ ] **Step 6: Verify application compiles**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

---

### Task 3: Data Models and DTOs

**Files:**
- Create: `backend/src/main/java/com/librarian/model/dto/ChatDto.java`
- Create: `backend/src/main/java/com/librarian/model/dto/DocumentDto.java`
- Create: `backend/src/main/java/com/librarian/model/dto/EvalDto.java`
- Create: `backend/src/main/java/com/librarian/model/entity/ConversationSession.java`
- Create: `backend/src/main/java/com/librarian/model/entity/Message.java`
- Create: `backend/src/main/java/com/librarian/model/entity/DocumentChunk.java`

- [ ] **Step 1: Create ChatDto.java**

Create `backend/src/main/java/com/librarian/model/dto/ChatDto.java`:

```java
package com.librarian.model.dto;

import java.util.List;

public class ChatDto {

    public record CreateSessionRequest(
            String title
    ) {}

    public record SessionResponse(
            String sessionId,
            String title,
            long messageCount,
            java.time.Instant createdAt,
            java.time.Instant lastActiveAt
    ) {}

    public record MessageRequest(
            String content
    ) {}

    public record MessageResponse(
            String role,
            String content,
            java.time.Instant timestamp,
            List<String> citations
    ) {}

    public record SessionWithMessages(
            String sessionId,
            String title,
            List<MessageResponse> messages,
            java.time.Instant createdAt
    ) {}
}
```

- [ ] **Step 2: Create DocumentDto.java**

Create `backend/src/main/java/com/librarian/model/dto/DocumentDto.java`:

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
            Instant createdAt,
            Instant processedAt
    ) {}
}
```

- [ ] **Step 3: Create EvalDto.java**

Create `backend/src/main/java/com/librarian/model/dto/EvalDto.java`:

```java
package com.librarian.model.dto;

import java.util.Map;

public class EvalDto {

    public record EvalConfig(
            int topK,
            boolean enableReranker,
            double temperature,
            String testSetPath
    ) {}

    public record EvalResult(
            double faithfulness,
            double contextPrecision,
            double accuracy,
            Map<String, Object> metrics,
            java.time.Instant completedAt
    ) {}

    public record CostReport(
            long totalInputTokens,
            long totalOutputTokens,
            double estimatedCostPer1000Calls,
            Map<String, Double> sensitivityAnalysis
    ) {}
}
```

- [ ] **Step 4: Create ConversationSession.java**

Create `backend/src/main/java/com/librarian/model/entity/ConversationSession.java`:

```java
package com.librarian.model.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConversationSession {

    private final String sessionId;
    private String title;
    private final List<Message> history;
    private Instant createdAt;
    private Instant lastActiveAt;

    public ConversationSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.history = new ArrayList<>();
        this.createdAt = Instant.now();
        this.lastActiveAt = Instant.now();
    }

    public ConversationSession(String title) {
        this();
        this.title = title;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Message> getHistory() {
        return history;
    }

    public void addMessage(Message message) {
        this.history.add(message);
        this.lastActiveAt = Instant.now();
    }

    public List<Message> getRecentHistory(int rounds) {
        int size = history.size();
        int maxMessages = rounds * 2; // user + assistant per round
        if (size <= maxMessages) {
            return history;
        }
        return history.subList(size - maxMessages, size);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public int getMessageCount() {
        return history.size();
    }
}
```

- [ ] **Step 5: Create Message.java**

Create `backend/src/main/java/com/librarian/model/entity/Message.java`:

```java
package com.librarian.model.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Message {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";

    private String role;
    private String content;
    private Instant timestamp;
    private List<String> citations;

    public Message() {
        this.timestamp = Instant.now();
        this.citations = new ArrayList<>();
    }

    public Message(String role, String content) {
        this();
        this.role = role;
        this.content = content;
    }

    public Message(String role, String content, List<String> citations) {
        this(role, content);
        this.citations = citations != null ? citations : new ArrayList<>();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public List<String> getCitations() {
        return citations;
    }

    public void setCitations(List<String> citations) {
        this.citations = citations;
    }
}
```

- [ ] **Step 6: Create DocumentChunk.java**

Create `backend/src/main/java/com/librarian/model/entity/DocumentChunk.java`:

```java
package com.librarian.model.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DocumentChunk {

    private final String chunkId;
    private String documentId;
    private String content;
    private Map<String, Object> metadata;

    public DocumentChunk() {
        this.chunkId = UUID.randomUUID().toString();
        this.metadata = new HashMap<>();
    }

    public DocumentChunk(String documentId, String content) {
        this();
        this.documentId = documentId;
        this.content = content;
    }

    public String getChunkId() {
        return chunkId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public String getMetadataAsString(String key) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : null;
    }
}
```

- [ ] **Step 7: Verify models compile**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

---

### Task 4: Utility Classes

**Files:**
- Create: `backend/src/main/java/com/librarian/util/PiiSanitizer.java`
- Create: `backend/src/main/java/com/librarian/util/LoggerUtil.java`

- [ ] **Step 1: Create PiiSanitizer.java**

Create `backend/src/main/java/com/librarian/util/PiiSanitizer.java`:

```java
package com.librarian.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PiiSanitizer {

    private static final Pattern EMAIL_PATTERN = 
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = 
            Pattern.compile("\\b1[3-9]\\d{9}\\b");
    private static final Pattern ID_CARD_PATTERN = 
            Pattern.compile("\\b\\d{17}[0-9Xx]\\b");

    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        result = EMAIL_PATTERN.matcher(result).replaceAll("[邮箱]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[电话]");
        result = ID_CARD_PATTERN.matcher(result).replaceAll("[身份证号]");

        return result;
    }

    public static String sanitizeForLog(String input) {
        String sanitized = sanitize(input);
        if (sanitized != null && sanitized.length() > 100) {
            return sanitized.substring(0, 100) + "...";
        }
        return sanitized;
    }
}
```

- [ ] **Step 2: Create LoggerUtil.java**

Create `backend/src/main/java/com/librarian/util/LoggerUtil.java`:

```java
package com.librarian.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

public class LoggerUtil {

    private static final String REQUEST_ID_KEY = "requestId";

    public static String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static void setRequestId() {
        MDC.put(REQUEST_ID_KEY, generateRequestId());
    }

    public static void clearRequestId() {
        MDC.remove(REQUEST_ID_KEY);
    }

    public static String getRequestId() {
        return MDC.get(REQUEST_ID_KEY);
    }

    public static void logChatMetrics(Logger log, long retrievalTimeMs, 
                                      long generationTimeMs, int retrievedDocs, 
                                      double avgSimilarity) {
        log.info("Chat completed - retrieval_time_ms={}, generation_time_ms={}, " +
                        "total_time_ms={}, retrieved_docs={}, avg_similarity={}",
                retrievalTimeMs, generationTimeMs, 
                retrievalTimeMs + generationTimeMs,
                retrievedDocs, String.format("%.2f", avgSimilarity));
    }
}
```

- [ ] **Step 3: Write unit test for PiiSanitizer**

Create `backend/src/test/java/com/librarian/util/PiiSanitizerTest.java`:

```java
package com.librarian.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PiiSanitizerTest {

    @Test
    void shouldSanitizeEmail() {
        String input = "Contact user@example.com for help";
        String result = PiiSanitizer.sanitize(input);
        assertEquals("Contact [邮箱] for help", result);
    }

    @Test
    void shouldSanitizePhone() {
        String input = "Call 13812345678 for support";
        String result = PiiSanitizer.sanitize(input);
        assertEquals("Call [电话] for support", result);
    }

    @Test
    void shouldSanitizeIdCard() {
        String input = "ID: 110101199001011234";
        String result = PiiSanitizer.sanitize(input);
        assertEquals("ID: [身份证号]", result);
    }

    @Test
    void shouldReturnNullForNull() {
        assertNull(PiiSanitizer.sanitize(null));
    }

    @Test
    void shouldReturnEmptyForEmpty() {
        assertEquals("", PiiSanitizer.sanitize(""));
    }

    @Test
    void shouldTruncateForLog() {
        String longText = "A".repeat(150);
        String result = PiiSanitizer.sanitizeForLog(longText);
        assertTrue(result.length() <= 103); // 100 + "..."
        assertTrue(result.endsWith("..."));
    }
}
```

- [ ] **Step 4: Run PiiSanitizer tests**

Run: `cd backend && mvn test -Dtest=PiiSanitizerTest`
Expected: BUILD SUCCESS, 6 tests passed

---

### Task 5: Controller Stubs

**Files:**
- Create: `backend/src/main/java/com/librarian/controller/ChatController.java`
- Create: `backend/src/main/java/com/librarian/controller/DocumentController.java`
- Create: `backend/src/main/java/com/librarian/controller/EvalController.java`

- [ ] **Step 1: Create ChatController.java**

Create `backend/src/main/java/com/librarian/controller/ChatController.java`:

```java
package com.librarian.controller;

import com.librarian.model.dto.ChatDto.*;
import com.librarian.service.ChatService;
import com.librarian.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/sessions")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public SessionResponse createSession(@RequestBody(required = false) CreateSessionRequest request) {
        LoggerUtil.setRequestId();
        log.info("Creating new chat session");
        SessionResponse response = chatService.createSession(request);
        log.info("Session created: {}", response.sessionId());
        return response;
    }

    @GetMapping
    public List<SessionResponse> listSessions() {
        LoggerUtil.setRequestId();
        log.info("Listing all sessions");
        return chatService.listSessions();
    }

    @GetMapping("/{sessionId}")
    public SessionWithMessages getSession(@PathVariable String sessionId) {
        LoggerUtil.setRequestId();
        log.info("Getting session: {}", sessionId);
        return chatService.getSession(sessionId);
    }

    @PostMapping("/{sessionId}/message")
    public MessageResponse sendMessage(@PathVariable String sessionId, 
                                       @RequestBody MessageRequest request) {
        LoggerUtil.setRequestId();
        log.info("Sending message to session: {}", sessionId);
        return chatService.sendMessage(sessionId, request);
    }

    @PostMapping(value = "/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@PathVariable String sessionId,
                                    @RequestBody MessageRequest request) {
        LoggerUtil.setRequestId();
        log.info("Starting stream for session: {}", sessionId);
        return chatService.streamMessage(sessionId, request);
    }
}
```

- [ ] **Step 2: Create DocumentController.java**

Create `backend/src/main/java/com/librarian/controller/DocumentController.java`:

```java
package com.librarian.controller;

import com.librarian.model.dto.DocumentDto.DocumentResponse;
import com.librarian.service.IngestionService;
import com.librarian.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

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
    public List<DocumentResponse> listDocuments() {
        LoggerUtil.setRequestId();
        log.info("Listing all documents");
        return ingestionService.listDocuments();
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
}
```

- [ ] **Step 3: Create EvalController.java**

Create `backend/src/main/java/com/librarian/controller/EvalController.java`:

```java
package com.librarian.controller;

import com.librarian.model.dto.EvalDto.*;
import com.librarian.service.EvalService;
import com.librarian.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eval")
public class EvalController {

    private static final Logger log = LoggerFactory.getLogger(EvalController.class);

    private final EvalService evalService;

    public EvalController(EvalService evalService) {
        this.evalService = evalService;
    }

    @PostMapping("/run")
    public String runEvaluation(@RequestBody(required = false) EvalConfig config) {
        LoggerUtil.setRequestId();
        log.info("Starting evaluation");
        evalService.runEvaluation(config);
        return "Evaluation started";
    }

    @GetMapping("/results")
    public EvalResult getResults() {
        LoggerUtil.setRequestId();
        log.info("Getting evaluation results");
        return evalService.getResults();
    }

    @GetMapping("/cost-estimate")
    public CostReport getCostEstimate(
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "false") boolean enableReranker,
            @RequestParam(defaultValue = "0.7") double temperature) {
        LoggerUtil.setRequestId();
        log.info("Getting cost estimate: topK={}, reranker={}, temp={}", 
                topK, enableReranker, temperature);
        return evalService.getCostEstimate(topK, enableReranker, temperature);
    }
}
```

- [ ] **Step 4: Verify controllers compile**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

---

### Task 6: Service Stubs

**Files:**
- Create: `backend/src/main/java/com/librarian/service/ChatService.java`
- Create: `backend/src/main/java/com/librarian/service/IngestionService.java`
- Create: `backend/src/main/java/com/librarian/service/EvalService.java`

- [ ] **Step 1: Create ChatService.java (stub with session management)**

Create `backend/src/main/java/com/librarian/service/ChatService.java`:

```java
package com.librarian.service;

import com.librarian.config.AiConfig;
import com.librarian.model.dto.ChatDto.*;
import com.librarian.model.entity.ConversationSession;
import com.librarian.model.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();
    private final AiConfig aiConfig;

    public ChatService(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    public SessionResponse createSession(CreateSessionRequest request) {
        String title = request != null ? request.title() : "New Conversation";
        ConversationSession session = new ConversationSession(title);
        sessions.put(session.getSessionId(), session);
        return toSessionResponse(session);
    }

    public List<SessionResponse> listSessions() {
        return sessions.values().stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    public SessionWithMessages getSession(String sessionId) {
        ConversationSession session = sessions.get(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }
        List<MessageResponse> messages = session.getHistory().stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
        return new SessionWithMessages(
                session.getSessionId(),
                session.getTitle(),
                messages,
                session.getCreatedAt()
        );
    }

    public MessageResponse sendMessage(String sessionId, MessageRequest request) {
        ConversationSession session = sessions.get(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }

        Message userMessage = new Message(Message.ROLE_USER, request.content());
        session.addMessage(userMessage);

        // TODO: Implement RAG pipeline integration
        Message assistantMessage = new Message(Message.ROLE_ASSISTANT, 
                "RAG pipeline not yet implemented. This is a placeholder response.");
        session.addMessage(assistantMessage);

        return toMessageResponse(assistantMessage);
    }

    public SseEmitter streamMessage(String sessionId, MessageRequest request) {
        SseEmitter emitter = new SseEmitter(30000L); // 30 second timeout

        // TODO: Implement streaming RAG pipeline
        try {
            emitter.send(SseEmitter.event().name("message").data("Streaming not yet implemented"));
            emitter.send(SseEmitter.event().name("done").data("done"));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @Scheduled(fixedRate = 600000) // Every 10 minutes
    public void cleanupExpiredSessions() {
        Instant timeout = Instant.now().minusSeconds(
                aiConfig.getMaxHistoryRounds() * 600L); // Placeholder timeout
        sessions.entrySet().removeIf(entry -> 
                entry.getValue().getLastActiveAt().isBefore(timeout));
    }

    private SessionResponse toSessionResponse(ConversationSession session) {
        return new SessionResponse(
                session.getSessionId(),
                session.getTitle(),
                session.getMessageCount(),
                session.getCreatedAt(),
                session.getLastActiveAt()
        );
    }

    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getRole(),
                message.getContent(),
                message.getTimestamp(),
                message.getCitations()
        );
    }
}
```

- [ ] **Step 2: Create IngestionService.java (stub)**

Create `backend/src/main/java/com/librarian/service/IngestionService.java`:

```java
package com.librarian.service;

import com.librarian.model.dto.DocumentDto.DocumentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final Map<String, DocumentResponse> documents = new ConcurrentHashMap<>();

    @Async("documentIngestionExecutor")
    public void ingestDocument(MultipartFile file) {
        String documentId = UUID.randomUUID().toString();
        log.info("Starting document ingestion: {} (id={})", 
                file.getOriginalFilename(), documentId);

        DocumentResponse doc = new DocumentResponse(
                documentId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                "processing",
                0,
                Instant.now(),
                null
        );
        documents.put(documentId, doc);

        try {
            // TODO: Implement actual parsing, chunking, embedding pipeline
            Thread.sleep(1000); // Simulate processing

            DocumentResponse completed = new DocumentResponse(
                    documentId,
                    doc.fileName(),
                    doc.fileType(),
                    doc.fileSize(),
                    "completed",
                    0,
                    doc.createdAt(),
                    Instant.now()
            );
            documents.put(documentId, completed);
            log.info("Document ingestion completed: {}", documentId);
        } catch (Exception e) {
            log.error("Document ingestion failed: {}", documentId, e);
            DocumentResponse failed = new DocumentResponse(
                    documentId, doc.fileName(), doc.fileType(),
                    doc.fileSize(), "failed", 0, doc.createdAt(), null
            );
            documents.put(documentId, failed);
        }
    }

    public List<DocumentResponse> listDocuments() {
        return new ArrayList<>(documents.values());
    }

    public DocumentResponse getDocument(String documentId) {
        DocumentResponse doc = documents.get(documentId);
        if (doc == null) {
            throw new RuntimeException("Document not found: " + documentId);
        }
        return doc;
    }

    public void deleteDocument(String documentId) {
        documents.remove(documentId);
        log.info("Document deleted: {}", documentId);
    }
}
```

- [ ] **Step 3: Create EvalService.java (stub)**

Create `backend/src/main/java/com/librarian/service/EvalService.java`:

```java
package com.librarian.service;

import com.librarian.model.dto.EvalDto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class EvalService {

    private static final Logger log = LoggerFactory.getLogger(EvalService.class);

    private EvalResult lastResult;

    public void runEvaluation(EvalConfig config) {
        log.info("Running evaluation with config: {}", config);
        // TODO: Implement actual evaluation logic
        lastResult = new EvalResult(
                0.0, 0.0, 0.0,
                Map.of("status", "not_implemented"),
                Instant.now()
        );
    }

    public EvalResult getResults() {
        if (lastResult == null) {
            return new EvalResult(
                    0.0, 0.0, 0.0,
                    Map.of("status", "no_eval_run"),
                    null
            );
        }
        return lastResult;
    }

    public CostReport getCostEstimate(int topK, boolean enableReranker, double temperature) {
        log.info("Estimating cost: topK={}, reranker={}, temp={}", topK, enableReranker, temperature);
        
        // TODO: Implement actual cost estimation
        Map<String, Double> sensitivity = new HashMap<>();
        sensitivity.put("top_k_" + topK, 0.0);
        sensitivity.put("reranker_" + enableReranker, 0.0);
        sensitivity.put("temperature_" + temperature, 0.0);

        return new CostReport(
                0, 0, 0.0, sensitivity
        );
    }
}
```

- [ ] **Step 4: Add @EnableScheduling to main application class**

Modify `backend/src/main/java/com/librarian/LibrarianAgentApplication.java`:

```java
package com.librarian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LibrarianAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibrarianAgentApplication.class, args);
    }
}
```

- [ ] **Step 5: Verify full compilation**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

---

### Task 7: RAG Pipeline Stub Classes

**Files:**
- Create: `backend/src/main/java/com/librarian/service/rag/DocumentParser.java`
- Create: `backend/src/main/java/com/librarian/service/rag/TextChunker.java`
- Create: `backend/src/main/java/com/librarian/service/rag/VectorSearch.java`
- Create: `backend/src/main/java/com/librarian/service/rag/ContextBuilder.java`
- Create: `backend/src/main/java/com/librarian/service/rag/QueryRewriter.java`
- Create: `backend/src/main/java/com/librarian/service/rag/LlmGenerator.java`

- [ ] **Step 1: Create DocumentParser.java (stub)**

Create `backend/src/main/java/com/librarian/service/rag/DocumentParser.java`:

```java
package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Component
public class DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(DocumentParser.class);

    public List<DocumentChunk> parse(MultipartFile file) {
        String filename = file.getOriginalFilename();
        log.info("Parsing document: {}", filename);

        if (filename == null) {
            return Collections.emptyList();
        }

        if (filename.toLowerCase().endsWith(".pdf")) {
            return parsePdf(file);
        } else if (filename.toLowerCase().endsWith(".md") || 
                   filename.toLowerCase().endsWith(".txt")) {
            return parseMarkdown(file);
        }

        log.warn("Unsupported file format: {}", filename);
        return Collections.emptyList();
    }

    private List<DocumentChunk> parsePdf(MultipartFile file) {
        // TODO: Implement PDF parsing with Apache PDFBox
        log.info("PDF parsing not yet implemented");
        return Collections.emptyList();
    }

    private List<DocumentChunk> parseMarkdown(MultipartFile file) {
        // TODO: Implement Markdown/TXT parsing
        log.info("Markdown/TXT parsing not yet implemented");
        return Collections.emptyList();
    }
}
```

- [ ] **Step 2: Create TextChunker.java (stub)**

Create `backend/src/main/java/com/librarian/service/rag/TextChunker.java`:

```java
package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class TextChunker {

    private static final Logger log = LoggerFactory.getLogger(TextChunker.class);

    @Value("${rag.chunk-size}")
    private int chunkSize;

    @Value("${rag.chunk-overlap}")
    private int chunkOverlap;

    public List<DocumentChunk> chunk(DocumentChunk document) {
        log.info("Chunking document: {}", document.getChunkId());
        // TODO: Implement chunking logic with overlap
        return Collections.singletonList(document);
    }

    public List<DocumentChunk> chunkAll(List<DocumentChunk> documents) {
        return documents.stream()
                .map(this::chunk)
                .flatMap(List::stream)
                .toList();
    }
}
```

- [ ] **Step 3: Create VectorSearch.java (stub)**

Create `backend/src/main/java/com/librarian/service/rag/VectorSearch.java`:

```java
package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class VectorSearch {

    private static final Logger log = LoggerFactory.getLogger(VectorSearch.class);

    private final EmbeddingModel embeddingModel;

    @Value("${rag.top-k}")
    private int topK;

    @Value("${rag.similarity-threshold}")
    private double similarityThreshold;

    public VectorSearch(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<DocumentChunk> search(String query) {
        log.info("Searching for: {} (topK={})", query, topK);
        // TODO: Implement vector search with ChromaDB
        return Collections.emptyList();
    }

    public List<DocumentChunk> search(String query, int topK) {
        log.info("Searching for: {} (topK={})", query, topK);
        return Collections.emptyList();
    }
}
```

- [ ] **Step 4: Create ContextBuilder.java (stub)**

Create `backend/src/main/java/com/librarian/service/rag/ContextBuilder.java`:

```java
package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(ContextBuilder.class);

    public String build(List<DocumentChunk> chunks) {
        log.info("Building context from {} chunks", chunks.size());
        // TODO: Format chunks into prompt context with citations
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            context.append("[来源: ").append(chunk.getMetadataAsString("fileName"))
                   .append("]\n")
                   .append(chunk.getContent())
                   .append("\n\n");
        }
        return context.toString();
    }
}
```

- [ ] **Step 5: Create QueryRewriter.java (stub)**

Create `backend/src/main/java/com/librarian/service/rag/QueryRewriter.java`:

```java
package com.librarian.service.rag;

import com.librarian.model.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QueryRewriter {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriter.class);

    public String rewrite(String query, List<Message> history) {
        log.info("Rewriting query with {} history messages", history.size());
        // TODO: Implement query rewriting using conversation history
        return query;
    }
}
```

- [ ] **Step 6: Create LlmGenerator.java (stub)**

Create `backend/src/main/java/com/librarian/service/rag/LlmGenerator.java`:

```java
package com.librarian.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class LlmGenerator {

    private static final Logger log = LoggerFactory.getLogger(LlmGenerator.class);

    private final ChatClient chatClient;

    public LlmGenerator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String generate(String context, String question) {
        log.info("Generating answer with context length: {}", context.length());
        // TODO: Implement LLM generation with prompt template
        return "Answer generation not yet implemented.";
    }

    public Flux<String> generateStream(String context, String question) {
        log.info("Generating streaming answer with context length: {}", context.length());
        // TODO: Implement streaming LLM generation
        return Flux.just("Streaming answer not yet implemented.");
    }
}
```

- [ ] **Step 7: Verify all stubs compile**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

---

### Task 8: RAG System Prompt Template

**Files:**
- Create: `backend/src/main/resources/prompts/rag-system-prompt.st`

- [ ] **Step 1: Create RAG system prompt**

Create `backend/src/main/resources/prompts/rag-system-prompt.st`:

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

- [ ] **Step 2: Verify resources are included in build**

Run: `cd backend && mvn process-resources`
Expected: BUILD SUCCESS, prompt file copied to target

---

### Task 9: Final Verification and README

**Files:**
- Create: `backend/README.md`
- Modify: `c:\Project\Code\LibrarianAgent\.gitignore`

- [ ] **Step 1: Run full Maven build**

Run: `cd backend && mvn clean package -DskipTests`
Expected: BUILD SUCCESS, JAR file created in target/

- [ ] **Step 2: Create backend README**

Create `backend/README.md`:

```markdown
# LibrarianAgent Backend

RAG-based Knowledge Base Q&A System backend built with Spring Boot 3.x and Spring AI.

## Prerequisites

- Java 17+
- Maven 3.8+
- LLM API key (configured via environment variable)

## Quick Start

```bash
# Set required environment variables
export LLM_API_KEY=your-api-key-here
export LLM_BASE_URL=https://open.bigmodel.cn/api/paas/v4/
export LLM_MODEL=glm-4

# Build the project
mvn clean package -DskipTests

# Run the application
mvn spring-boot:run
```

The server will start on http://localhost:8080

## Configuration

All configuration is in `src/main/resources/application.yml`. Key settings can be overridden with environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| LLM_BASE_URL | LLM API endpoint | https://open.bigmodel.cn/api/paas/v4/ |
| LLM_API_KEY | API authentication key | (required) |
| LLM_MODEL | Model name | glm-4 |
| LLM_TEMPERATURE | Generation temperature | 0.7 |
| SERVER_PORT | Server port | 8080 |

## API Endpoints

- POST `/api/v1/chat/sessions` - Create chat session
- GET `/api/v1/chat/sessions` - List sessions
- POST `/api/v1/chat/sessions/{id}/message` - Send message
- POST `/api/v1/documents/upload` - Upload document
- GET `/api/v1/eval/results` - Get evaluation results

## Project Structure

```
src/main/java/com/librarian/
├── config/          # Spring configurations
├── controller/      # REST API controllers
├── service/         # Business logic services
│   └── rag/         # RAG pipeline components
├── model/           # Data models and DTOs
│   ├── dto/         # Request/Response DTOs
│   └── entity/      # Domain entities
└── util/            # Utility classes
```
```

- [ ] **Step 3: Update root .gitignore**

Read the current `.gitignore` at `c:\Project\Code\LibrarianAgent\.gitignore` and ensure it contains:

```
# Maven
backend/target/

# IDE
.idea/
*.iml

# Environment
.env
*.env

# OS
.DS_Store
Thumbs.db
```

Run: `cd c:\Project\Code\LibrarianAgent && git add backend/ && git status`
Expected: All backend files staged for commit

---

## Self-Review Checklist

### Spec Coverage
- [x] Project structure matches design doc Section 6
- [x] All config classes present (AiConfig, ChromaConfig, AsyncConfig, WebMvcConfig)
- [x] All controllers match API design in Section 4
- [x] All service stubs created (ChatService, IngestionService, EvalService)
- [x] All RAG pipeline stubs created (DocumentParser, TextChunker, VectorSearch, ContextBuilder, QueryRewriter, LlmGenerator)
- [x] DTOs match API design in Section 4
- [x] Entity models match Section 3.3 design
- [x] Utility classes created (PiiSanitizer, LoggerUtil)
- [x] application.yml matches Section 7.1
- [x] Prompt template matches Section 3.2.4
- [x] Maven dependency management with correct versions

### Placeholder Scan
- No "TBD" or "TODO" in configuration
- Service stubs clearly marked as TODO with specific implementation pointers
- No "implement later" without context

### Type Consistency
- DTO record types consistent across controllers and models
- Entity types match between ConversationSession, Message, and DTOs
- Method signatures use consistent parameter names
