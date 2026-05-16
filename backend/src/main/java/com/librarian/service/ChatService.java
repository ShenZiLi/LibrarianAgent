package com.librarian.service;

import com.librarian.config.AiConfig;
import com.librarian.model.dto.ChatDto.*;
import com.librarian.model.entity.ConversationSession;
import com.librarian.model.entity.Message;
import com.librarian.model.entity.DocumentChunk;
import com.librarian.service.rag.*;
import com.librarian.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ChatService {

    private final java.util.Map<String, ConversationSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();
    private final AiConfig aiConfig;
    private final QueryRewriter queryRewriter;
    private final VectorSearch vectorSearch;
    private final ContextBuilder contextBuilder;
    private final LlmGenerator llmGenerator;

    public ChatService(AiConfig aiConfig,
                       QueryRewriter queryRewriter,
                       VectorSearch vectorSearch,
                       ContextBuilder contextBuilder,
                       LlmGenerator llmGenerator) {
        this.aiConfig = aiConfig;
        this.queryRewriter = queryRewriter;
        this.vectorSearch = vectorSearch;
        this.contextBuilder = contextBuilder;
        this.llmGenerator = llmGenerator;
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
        long startTime = System.currentTimeMillis();
        ConversationSession session = sessions.get(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }

        Message userMessage = new Message(Message.ROLE_USER, request.content());
        session.addMessage(userMessage);

        List<Message> history = session.getRecentHistory(aiConfig.getMaxHistoryRounds());

        String rewrittenQuery = queryRewriter.rewrite(request.content(), history);
        long retrievalStart = System.currentTimeMillis();
        List<DocumentChunk> searchResults = vectorSearch.search(rewrittenQuery);
        long retrievalTime = System.currentTimeMillis() - retrievalStart;

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

        Message assistantMessage = new Message(Message.ROLE_ASSISTANT, answer, citations);
        session.addMessage(assistantMessage);

        double avgSimilarity = searchResults.isEmpty() ? 0.0 : 0.8;
        LoggerUtil.logChatMetrics(log, retrievalTime, generationTime, searchResults.size(), avgSimilarity);

        return toMessageResponse(assistantMessage);
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

        List<Message> history = session.getRecentHistory(aiConfig.getMaxHistoryRounds());
        String rewrittenQuery = queryRewriter.rewrite(request.content(), history);

        List<DocumentChunk> searchResults = vectorSearch.search(rewrittenQuery);

        if (searchResults.isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("message")
                        .data("抱歉，我在知识库中没有找到与您的问题相关的信息。"));
                emitter.send(SseEmitter.event().name("citations").data("[]"));
                emitter.send(SseEmitter.event().name("done").data("done"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
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
                        emitter.send(SseEmitter.event().name("message").data(chunk));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> emitter.completeWithError(error),
                () -> {
                    try {
                        String citationsJson = citations.toString();
                        emitter.send(SseEmitter.event().name("citations").data(citationsJson));
                        emitter.send(SseEmitter.event().name("done").data("done"));
                        emitter.complete();

                        Message assistantMessage = new Message(Message.ROLE_ASSISTANT,
                                fullAnswer.toString(), citations);
                        session.addMessage(assistantMessage);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }
        );

        return emitter;
    }

    @Scheduled(fixedRate = 600000)
    public void cleanupExpiredSessions() {
        Instant timeout = Instant.now().minusSeconds(1800);
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
