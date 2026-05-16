package com.librarian.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class LlmGenerator {

    private static final Logger log = LoggerFactory.getLogger(LlmGenerator.class);

    private final ChatClient chatClient;

    public LlmGenerator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String generate(String context, String question) {
        log.info("Generating answer with context length: {}", context.length());
        return "Answer generation not yet implemented.";
    }

    public Flux<String> generateStream(String context, String question) {
        log.info("Generating streaming answer with context length: {}", context.length());
        return Flux.just("Streaming answer not yet implemented.");
    }
}
