# LibrarianAgent Backend

RAG-based Knowledge Base Q&A System backend built with Spring Boot 3.x and Spring AI.

## Prerequisites

- Java 17+
- Maven 3.8+
- LLM API key (configured via environment variable)

## Quick Start

```bash
# Set required environment variables
set LLM_API_KEY=your-api-key-here
set LLM_BASE_URL=https://open.bigmodel.cn/api/paas/v4/
set LLM_MODEL=glm-4

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
