package com.librarian.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AiConfig {

    public enum ModelProvider {
        ZHIPU,
        QWEN,
        MINIMAX
    }

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

    @Value("${ai.model.provider:ZHIPUAI}")
    private ModelProvider modelProvider;

    @Bean
    public OpenAiApi openAiApi(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi,
                                           @Value("${spring.ai.openai.chat.options.model:glm-4}") String model,
                                           @Value("${spring.ai.openai.chat.options.temperature:0.7}") Double temperature) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(
                        org.springframework.ai.openai.OpenAiChatOptions.builder()
                                .model(model)
                                .temperature(temperature)
                                .build()
                )
                .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    @Bean("queryRewriteChatClient")
    public ChatClient queryRewriteChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("You are a query rewriter for a RAG system. " +
                        "Given a conversation history and a follow-up question, " +
                        "rewrite the follow-up question as a standalone, self-contained query " +
                        "that can be used for document retrieval. " +
                        "Only output the rewritten query, nothing else.")
                .build();
    }

    @Bean("ragChatClient")
    public ChatClient ragChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个企业知识库助手。请基于提供的上下文回答问题。")
                .build();
    }

    @Bean
    public AiModelInfoPrinter aiModelInfoPrinter(
            @Value("${ai.model.provider:ZHIPUAI}") ModelProvider provider,
            @Value("${spring.ai.openai.chat.options.model:glm-4}") String chatModel,
            @Value("${spring.ai.openai.embedding.options.model:embedding-3}") String embeddingModel,
            @Value("${spring.ai.openai.base-url}") String baseUrl) {
        log.info("========== AI Model Configuration ==========");
        log.info("Provider     : {}", provider);
        log.info("Chat Model   : {}", chatModel);
        log.info("Embedding     : {}", embeddingModel);
        log.info("Base URL     : {}", baseUrl);
        log.info("================================================");
        return new AiModelInfoPrinter();
    }

    public static class AiModelInfoPrinter {
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

    public ModelProvider getModelProvider() {
        return modelProvider;
    }
}
