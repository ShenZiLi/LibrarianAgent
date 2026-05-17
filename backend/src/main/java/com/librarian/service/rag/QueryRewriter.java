package com.librarian.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class QueryRewriter {

    @Autowired
    @Qualifier("queryRewriteChatClient")
    private ChatClient chatClient;

    public String rewrite(String query, List<com.librarian.model.entity.Message> history) {
        if (history == null || history.isEmpty()) {
            log.debug("No history, returning original query");
            return query;
        }

        log.info("Rewriting query with {} history messages", history.size());

        StringBuilder historyBuilder = new StringBuilder();
        for (com.librarian.model.entity.Message msg : history) {
            String roleLabel = msg.getRole().equals("user") ? "User" : "Assistant";
            historyBuilder.append(roleLabel).append(": ").append(msg.getContent()).append("\n");
        }

        String rewrittenQuery = chatClient.prompt()
                .user(u -> u.text("""
                        Conversation History:
                        {history}

                        Follow-up Question: {question}

                        Standalone Query:
                        """)
                        .param("history", historyBuilder.toString())
                        .param("question", query))
                .call()
                .content();

        String result = rewrittenQuery != null ? rewrittenQuery.trim() : query;
        log.info("Rewritten query: {}", result);
        return result;
    }
}
