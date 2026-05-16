package com.librarian.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AiConfig {

    @Value("${rag.top-k:5}")
    private int topK;

    @Value("${rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${rag.max-history-rounds:10}")
    private int maxHistoryRounds;

    @Value("${rag.chunk-size:512}")
    private int chunkSize;

    @Value("${rag.chunk-overlap:128}")
    private int chunkOverlap;

    @Bean
    public ChatClient chatClient(ZhiPuAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean("queryRewriteChatClient")
    public ChatClient queryRewriteChatClient(ZhiPuAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("You are a query rewriter for a RAG system. " +
                        "Given a conversation history and a follow-up question, " +
                        "rewrite the follow-up question as a standalone, self-contained query " +
                        "that can be used for document retrieval. " +
                        "Only output the rewritten query, nothing else.")
                .build();
    }

    @Bean("ragChatClient")
    public ChatClient ragChatClient(ZhiPuAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个企业知识库助手。请基于提供的上下文回答问题。")
                .build();
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

    public int getChunkSize() {
        return chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }
}
