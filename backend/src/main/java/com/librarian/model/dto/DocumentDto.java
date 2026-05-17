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
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
