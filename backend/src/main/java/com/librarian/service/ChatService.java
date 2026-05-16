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
        SseEmitter emitter = new SseEmitter(30000L);

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
