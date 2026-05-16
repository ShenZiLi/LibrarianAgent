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
