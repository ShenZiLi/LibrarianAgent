package com.librarian.service;

import com.librarian.config.RagProperties;
import com.librarian.model.dto.ChatDto.*;
import com.librarian.model.entity.ConversationSession;
import com.librarian.model.entity.Message;
import com.librarian.model.entity.DocumentChunk;
import com.librarian.service.rag.*;
import com.librarian.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class ChatService {

    private final java.util.Map<String, ConversationSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();
    private final RagProperties ragProperties;
    private final QueryRewriter queryRewriter;
    private final VectorSearch vectorSearch;
    private final ContextBuilder contextBuilder;
    private final LlmGenerator llmGenerator;

    public ChatService(RagProperties ragProperties,
                       QueryRewriter queryRewriter,
                       VectorSearch vectorSearch,
                       ContextBuilder contextBuilder,
                       LlmGenerator llmGenerator) {
        this.ragProperties = ragProperties;
        this.queryRewriter = queryRewriter;
        this.vectorSearch = vectorSearch;
        this.contextBuilder = contextBuilder;
        this.llmGenerator = llmGenerator;
    }

    public SessionResponse createSession(CreateSessionRequest request) {
        String title = request != null ? request.title() : "New Conversation";
        ConversationSession session = new ConversationSession(title);
        sessions.put(session.getSessionId(), session);
        log.info("Session created: {} with title: {}", session.getSessionId(), title);
        return toSessionResponse(session);
    }

    public List<SessionResponse> listSessions() {
        return sessions.values().stream()
                .map(this::toSessionResponse)
                .toList();
    }

    public SessionWithMessages getSession(String sessionId) {
        ConversationSession session = sessions.get(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }
        List<MessageResponse> messages = session.getHistory().stream()
                .map(this::toMessageResponse)
                .toList();
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

        List<Message> history = session.getRecentHistory(ragProperties.getMaxHistoryRounds());

        try {
            String rewrittenQuery = queryRewriter.rewrite(request.content(), history);

            long retrievalStart = System.currentTimeMillis();
            List<DocumentChunk> searchResults = vectorSearch.search(rewrittenQuery);
            long retrievalTime = System.currentTimeMillis() - retrievalStart;
            log.debug("Retrieval completed in {}ms, found {} results", retrievalTime, searchResults.size());

            if (searchResults.isEmpty()) {
                Message assistantMessage = new Message(Message.ROLE_ASSISTANT,
                        "抱歉，我在知识库中没有找到与您的问题相关的信息。请尝试换一种方式提问，或上传相关文档到知识库中。");
                session.addMessage(assistantMessage);
                return toMessageResponse(assistantMessage);
            }

            String context = contextBuilder.build(searchResults);
            List<String> citations = contextBuilder.buildCitations(searchResults);

            long generationStart = System.currentTimeMillis();
            String answer = llmGenerator.generate(context, rewrittenQuery);
            long generationTime = System.currentTimeMillis() - generationStart;
            log.debug("Generation completed in {}ms", generationTime);

            Message assistantMessage = new Message(Message.ROLE_ASSISTANT, answer, citations);
            session.addMessage(assistantMessage);

            double avgSimilarity = calculateAvgSimilarity(searchResults);
            LoggerUtil.logChatMetrics(log, retrievalTime, generationTime, searchResults.size(), avgSimilarity);

            return toMessageResponse(assistantMessage);
        } catch (NonTransientAiException e) {
            log.error("AI model call failed: {}", e.getMessage(), e);
            Message assistantMessage = new Message(Message.ROLE_ASSISTANT,
                    "抱歉，AI服务暂时不可用，请稍后重试。错误信息: " + extractErrorMessage(e));
            session.addMessage(assistantMessage);
            return toMessageResponse(assistantMessage);
        } catch (Exception e) {
            log.error("Unexpected error during chat: {}", e.getMessage(), e);
            Message assistantMessage = new Message(Message.ROLE_ASSISTANT,
                    "抱歉，处理您的问题时出现了错误，请稍后重试。");
            session.addMessage(assistantMessage);
            return toMessageResponse(assistantMessage);
        }
    }

    public SseEmitter streamMessage(String sessionId, MessageRequest request) {
        SseEmitter emitter = new SseEmitter(60000L);

        ConversationSession session = sessions.get(sessionId);
        if (session == null) {
            emitter.completeWithError(new RuntimeException("Session not found: " + sessionId));
            return emitter;
        }

        Message userMessage = new Message(Message.ROLE_USER, request.content());
        session.addMessage(userMessage);

        List<Message> history = session.getRecentHistory(ragProperties.getMaxHistoryRounds());

        try {
            String rewrittenQuery = queryRewriter.rewrite(request.content(), history);
            List<DocumentChunk> searchResults = vectorSearch.search(rewrittenQuery);

            if (searchResults.isEmpty()) {
                sendSseMessage(emitter, "抱歉，我在知识库中没有找到与您的问题相关的信息。");
                sendSseEvent(emitter, "citations", "[]");
                sendSseEvent(emitter, "done", "done");
                emitter.complete();
                return emitter;
            }

            String context = contextBuilder.build(searchResults);
            List<String> citations = contextBuilder.buildCitations(searchResults);

            Flux<String> contentFlux = llmGenerator.generateStream(context, rewrittenQuery);
            StringBuilder fullAnswer = new StringBuilder();

            contentFlux.subscribe(
                    chunk -> {
                        try {
                            fullAnswer.append(chunk);
                            sendSseMessage(emitter, chunk);
                        } catch (IOException e) {
                            log.error("Failed to send SSE chunk: {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("SSE stream error: {}", error.getMessage());
                        try {
                            sendSseMessage(emitter, "\n\n[错误: 生成回答时出现问题]");
                            sendSseEvent(emitter, "done", "error");
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(error);
                        }
                    },
                    () -> {
                        try {
                            String citationsJson = citations.toString();
                            sendSseEvent(emitter, "citations", citationsJson);
                            sendSseEvent(emitter, "done", "done");
                            emitter.complete();

                            Message assistantMessage = new Message(Message.ROLE_ASSISTANT,
                                    fullAnswer.toString(), citations);
                            session.addMessage(assistantMessage);
                        } catch (IOException e) {
                            log.error("Failed to complete SSE stream: {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    }
            );
        } catch (NonTransientAiException e) {
            log.error("AI model call failed during streaming: {}", e.getMessage());
            try {
                sendSseMessage(emitter, "抱歉，AI服务暂时不可用，请稍后重试。");
                sendSseEvent(emitter, "done", "error");
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        } catch (Exception e) {
            log.error("Unexpected error during streaming: {}", e.getMessage());
            try {
                sendSseMessage(emitter, "抱歉，处理您的问题时出现了错误。");
                sendSseEvent(emitter, "done", "error");
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }

        return emitter;
    }

    private void sendSseMessage(SseEmitter emitter, String data) throws IOException {
        emitter.send(SseEmitter.event().name("message").data(data));
    }

    private void sendSseEvent(SseEmitter emitter, String name, String data) throws IOException {
        emitter.send(SseEmitter.event().name(name).data(data));
    }

    private double calculateAvgSimilarity(List<DocumentChunk> results) {
        if (results.isEmpty()) {
            return 0.0;
        }
        return results.stream()
                .mapToDouble(c -> c.getMetadataAsDouble("similarity", 0.8))
                .average()
                .orElse(0.8);
    }

    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message != null && message.length() > 100) {
            return message.substring(0, 100) + "...";
        }
        return message != null ? message : "未知错误";
    }

    @Scheduled(fixedRate = 600000)
    public void cleanupExpiredSessions() {
        Instant timeout = Instant.now().minusSeconds(1800);
        int beforeCount = sessions.size();
        sessions.entrySet().removeIf(entry ->
                entry.getValue().getLastActiveAt().isBefore(timeout));
        int removed = beforeCount - sessions.size();
        if (removed > 0) {
            log.info("Cleaned up {} expired sessions", removed);
        }
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
