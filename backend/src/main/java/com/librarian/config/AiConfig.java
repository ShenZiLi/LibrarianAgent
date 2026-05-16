package com.librarian.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${rag.top-k}")
    private int topK;

    @Value("${rag.similarity-threshold}")
    private double similarityThreshold;

    @Value("${rag.max-history-rounds}")
    private int maxHistoryRounds;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    public int getTopK() {
        return topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public int getMaxHistoryRounds() {
        return maxHistoryRounds;
    }
}
