package com.librarian.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Slf4j
@Configuration
public class ChatClientConfig {
    
    @Value("${spring.ai.openai.chat.options.model:glm-4}")
    private String chatModelName;

    @Value("${spring.ai.openai.embedding.options.model:embedding-3}")
    private String embeddingModelName;

    @Value("${spring.ai.openai.base-url:}")
    private String baseUrl;

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean("queryRewriteChatClient")
    public ChatClient queryRewriteChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("You are a query rewriter for a RAG system. " +
                        "Given a conversation history and a follow-up question, " +
                        "rewrite the follow-up question as a standalone, self-contained query " +
                        "that can be used for document retrieval. " +
                        "Only output the rewritten query, nothing else.")
                .build();
    }

    @Bean("ragChatClient")
    public ChatClient ragChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个企业知识库助手。请基于提供的上下文回答问题。")
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printModelInfo() {
        log.info("============================================");
        log.info("       AI Model Configuration");
        log.info("============================================");
        log.info("  Chat Model : {}", chatModelName);
        log.info("  Embedding  : {}", embeddingModelName);
        log.info("  Base URL   : {}", baseUrl);
        log.info("============================================");
    }
}
