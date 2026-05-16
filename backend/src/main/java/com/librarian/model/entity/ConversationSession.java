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
        int maxMessages = rounds * 2;
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
