package com.librarian.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private int chunkSize = 512;
    private int chunkOverlap = 128;
    private int topK = 5;
    private double similarityThreshold = 0.7;
    private int maxHistoryRounds = 10;
    private int sessionTimeoutMinutes = 30;
    private int processingTimeoutMinutes = 10;
    private int processingCheckInterval = 300000;
}
