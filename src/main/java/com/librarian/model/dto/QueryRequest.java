package com.librarian.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {

    private String sessionId;

    @NotBlank(message = "Query cannot be blank")
    private String query;

    @Builder.Default
    private Integer topK = 5;

    @Builder.Default
    private Double temperature = 0.1;

    @Builder.Default
    private Boolean enableReranker = true;
}
