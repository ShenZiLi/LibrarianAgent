package com.librarian.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedContext {

    private String chunkId;

    private String content;

    private Double score;

    private String sourceFile;

    private Integer pageNumber;

    private String sectionTitle;

    private String language;

    private String documentType;
}
