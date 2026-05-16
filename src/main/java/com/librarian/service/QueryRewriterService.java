package com.librarian.service;

import com.librarian.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriterService {

    private final ZhiPuAiChatModel chatModel;
    private final RagProperties ragProperties;

    public String rewrite(String query, List<Map<String, Object>> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return query;
        }

        int compressionTurns = ragProperties.getConversation().getCompressionTurns();
        List<Map<String, Object>> recentMessages = conversationHistory.size() > compressionTurns * 2
                ? conversationHistory.subList(conversationHistory.size() - compressionTurns * 2, conversationHistory.size())
                : conversationHistory;

        StringBuilder historyBuilder = new StringBuilder();
        for (Map<String, Object> msg : recentMessages) {
            String role = (String) msg.get("role");
            String content = (String) msg.get("content");
            if ("user".equals(role)) {
                historyBuilder.append("用户: ").append(content).append("\n");
            } else if ("assistant".equals(role)) {
                historyBuilder.append("助手: ").append(content).append("\n");
            }
        }

        String prompt = String.format("""
                根据以下对话历史，将用户的最新问题重写为一个独立的检索查询，以便从知识库中检索相关信息。
                要求：
                1. 消除指代词（如"它"、"这个"、"那家公司"等），替换为具体实体。
                2. 补充对话历史中提及的关键上下文。
                3. 输出仅包含重写后的查询文本，不要包含任何额外说明。

                === 对话历史 ===
                %s

                === 最新问题 ===
                %s

                === 重写后的独立检索查询 ===
                """, historyBuilder.toString(), query);

        try {
            String rewrittenQuery = chatModel.call(prompt);
            String trimmed = rewrittenQuery != null ? rewrittenQuery.trim() : query;
            log.debug("Query rewritten: '{}' -> '{}'", query, trimmed);
            return trimmed;
        } catch (Exception e) {
            log.error("Query rewriting failed, using original query", e);
            return query;
        }
    }
}
