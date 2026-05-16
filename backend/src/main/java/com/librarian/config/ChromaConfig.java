package com.librarian.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChromaConfig {

    @Value("${chroma.host}")
    private String host;

    @Value("${chroma.port}")
    private int port;

    @Value("${chroma.collection-name}")
    private String collectionName;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getCollectionName() {
        return collectionName;
    }
}
