package com.librarian.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarian.config.RagProperties;
import com.librarian.model.dto.RetrievedContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;

    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String MESSAGES_KEY = "messages";
    private static final String CONTEXT_KEY = "context";

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        String key = SESSION_KEY_PREFIX + sessionId;

        Map<String, Object> sessionData = Map.of(
                MESSAGES_KEY, Collections.emptyList(),
                "created_at", System.currentTimeMillis()
        );

        redisTemplate.opsForHash().putAll(key, sessionData);
        redisTemplate.expire(key, ragProperties.getConversation().getRedisTtl(), TimeUnit.SECONDS);

        log.debug("Created session: {}", sessionId);
        return sessionId;
    }

    public void addMessage(String sessionId, String role, String content) {
        String key = SESSION_KEY_PREFIX + sessionId;

        List<Map<String, Object>> messages = getMessages(sessionId);

        Map<String, Object> message = Map.of(
                "role", role,
                "content", content,
                "timestamp", System.currentTimeMillis()
        );
        messages.add(message);

        int maxTurns = ragProperties.getConversation().getMaxTurns();
        if (messages.size() > maxTurns * 2) {
            messages = messages.subList(messages.size() - maxTurns * 2, messages.size());
        }

        try {
            String messagesJson = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForHash().put(key, MESSAGES_KEY, messagesJson);
        } catch (Exception e) {
            log.error("Failed to serialize messages for session: {}", sessionId, e);
        }

        redisTemplate.expire(key, ragProperties.getConversation().getRedisTtl(), TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMessages(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        Object messagesObj = redisTemplate.opsForHash().get(key, MESSAGES_KEY);

        if (messagesObj == null) {
            return new ArrayList<>();
        }

        try {
            if (messagesObj instanceof String) {
                return objectMapper.readValue((String) messagesObj, List.class);
            }
        } catch (Exception e) {
            log.error("Failed to deserialize messages for session: {}", sessionId, e);
        }

        return new ArrayList<>();
    }

    public List<Map<String, Object>> getRecentMessages(String sessionId, int turns) {
        List<Map<String, Object>> allMessages = getMessages(sessionId);
        int maxMessages = turns * 2;
        if (allMessages.size() > maxMessages) {
            return allMessages.subList(allMessages.size() - maxMessages, allMessages.size());
        }
        return allMessages;
    }

    public void deleteSession(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        redisTemplate.delete(key);
        log.debug("Deleted session: {}", sessionId);
    }

    public boolean sessionExists(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
