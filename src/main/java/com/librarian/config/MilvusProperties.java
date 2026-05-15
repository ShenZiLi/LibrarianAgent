package com.librarian.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {

    private String host = "localhost";
    private int port = 19530;
    private String collectionName = "rag_knowledge_base";
    private int embeddingDimension = 1024;
    private Hnsw hnsw = new Hnsw();
    private Search search = new Search();

    @Data
    public static class Hnsw {
        private int m = 16;
        private int efConstruction = 200;
    }

    @Data
    public static class Search {
        private int ef = 50;
        private int topK = 5;
    }
}
