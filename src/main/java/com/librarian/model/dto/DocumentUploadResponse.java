package com.librarian.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {

    private String documentId;

    private String status;

    private Long estimatedSeconds;

    private Integer chunkCount;
}
