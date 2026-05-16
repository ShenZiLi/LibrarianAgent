package com.librarian.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class LlmGenerator {

    private final ChatClient chatClient;
    private final Resource promptResource;

    public LlmGenerator(ChatClient chatClient,
                        @Value("classpath:prompts/rag-system-prompt.st") Resource promptResource) {
        this.chatClient = chatClient;
        this.promptResource = promptResource;
    }

    public String generate(String context, String question) {
        log.info("Generating answer with context length: {}", context.length());

        String systemPrompt = loadSystemPrompt();

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(u -> u.text("""
                        上下文:
                        {context}

                        问题: {question}
                        """)
                        .param("context", context)
                        .param("question", question))
                .call()
                .content();

        return answer != null ? answer : "抱歉，无法生成回答。";
    }

    public Flux<String> generateStream(String context, String question) {
        log.info("Generating streaming answer with context length: {}", context.length());

        String systemPrompt = loadSystemPrompt();

        return chatClient.prompt()
                .system(systemPrompt)
                .user(u -> u.text("""
                        上下文:
                        {context}

                        问题: {question}
                        """)
                        .param("context", context)
                        .param("question", question))
                .stream()
                .content();
    }

    private String loadSystemPrompt() {
        try {
            return new String(promptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to load prompt template, using default", e);
            return """
                    你是一个企业知识库助手。请基于以下检索到的上下文回答问题。

                    规则:
                    1. 答案必须严格基于提供的上下文
                    2. 如果上下文不足以回答问题，请明确说明
                    3. 在每个事实陈述后标注引用来源，格式如 [来源: 文档名, 章节]
                    4. 如果检测到个人身份信息，请进行脱敏处理
                    """;
        }
    }
}
