package com.librarian.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private Chunk chunk = new Chunk();
    private Reranker reranker = new Reranker();
    private double similarityThreshold = 0.3;
    private int maxContextTokens = 4096;
    private Conversation conversation = new Conversation();

    @Data
    public static class Chunk {
        private int targetSize = 512;
        private int overlap = 128;
        private int minSize = 256;
        private int maxSize = 768;
    }

    @Data
    public static class Reranker {
        private boolean enabled = true;
        private int topN = 5;
        private String url = "http://localhost:8001/rerank";
    }

    @Data
    public static class Conversation {
        private int maxTurns = 10;
        private int compressionTurns = 3;
        private long redisTtl = 3600;
    }
}
