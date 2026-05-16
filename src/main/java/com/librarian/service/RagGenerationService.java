package com.librarian.service;

import com.librarian.config.RagProperties;
import com.librarian.model.dto.RetrievedContext;
import com.librarian.model.dto.AnswerResponse;
import com.librarian.security.PiiMasker;
import com.librarian.security.PromptInjectionGuard;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagGenerationService {

    private final ZhiPuAiChatModel chatModel;
    private final RagProperties ragProperties;
    private final PiiMasker piiMasker;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是一个专业的企业内部知识问答助手。请根据以下检索到的上下文来回答问题。

            规则：
            1. 你的答案必须严格基于以下检索内容，不得使用外部知识。
            2. 如果检索内容中没有足够的信息来回答问题，请明确说明"我在知识库中没有找到相关信息"。
            3. 引用来源格式：[来源：文件名，页码X]
            4. 使用与用户提问相同的语言回答。
            5. 保持回答简洁、准确。

            === 检索上下文 ===
            %s
            === 检索上下文结束 ===
            """;

    public AnswerResponse generateAnswer(String query, List<RetrievedContext> contexts,
                                         List<Message> conversationHistory,
                                         Double temperature, String traceId) {
        long startTime = System.currentTimeMillis();

        String contextText = buildContextText(contexts);
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, contextText);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.addAll(conversationHistory);
        messages.add(new UserMessage(query));

        ChatOptions options = ChatOptions.builder()
                .model("glm-4.6v")
                .temperature(temperature)
                .maxTokens(ragProperties.getMaxContextTokens() / 2)
                .build();

        Prompt prompt = new Prompt(messages, options);
        ChatResponse chatResponse = chatModel.call(prompt);
        String answer = chatResponse.getResult().getOutput().getText();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("LLM generation completed in {}ms, model={}", elapsed, "glm-4.6v");

        return AnswerResponse.builder()
                .traceId(traceId)
                .answer(piiMasker.mask(answer))
                .citations(buildCitations(contexts))
                .confidence(calculateConfidence(contexts))
                .latencyMs(elapsed)
                .rejected(false)
                .build();
    }

    private String buildContextText(List<RetrievedContext> contexts) {
        StringBuilder sb = new StringBuilder();
        int totalTokens = 0;
        int maxTokens = ragProperties.getMaxContextTokens();

        for (int i = 0; i < contexts.size(); i++) {
            RetrievedContext ctx = contexts.get(i);
            String block = String.format("[来源: %s, 页码: %s]\n%s\n\n",
                    ctx.getSourceFile(),
                    ctx.getPageNumber() != null ? ctx.getPageNumber() : "N/A",
                    ctx.getContent());

            int blockTokens = estimateTokenCount(block);
            if (totalTokens + blockTokens > maxTokens) {
                break;
            }

            sb.append(block);
            totalTokens += blockTokens;
        }

        return sb.toString();
    }

    private List<AnswerResponse.Citation> buildCitations(List<RetrievedContext> contexts) {
        return contexts.stream()
                .map(ctx -> AnswerResponse.Citation.builder()
                        .source(ctx.getSourceFile())
                        .page(ctx.getPageNumber())
                        .snippet(truncate(ctx.getContent(), 200))
                        .score(ctx.getScore())
                        .build())
                .toList();
    }

    private Double calculateConfidence(List<RetrievedContext> contexts) {
        if (contexts.isEmpty()) {
            return 0.0;
        }
        double maxScore = contexts.stream()
                .mapToDouble(RetrievedContext::getScore)
                .max()
                .orElse(0.0);
        return Math.min(maxScore, 1.0);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private int estimateTokenCount(String text) {
        if (text == null) {
            return 0;
        }
        int chineseCharCount = 0;
        int otherCharCount = 0;
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                chineseCharCount++;
            } else {
                otherCharCount++;
            }
        }
        return (int) Math.ceil(chineseCharCount * 0.5 + otherCharCount * 0.25);
    }
}
