package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class QueryRewriter {

    private final ChatClient chatClient;

    public QueryRewriter(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String rewrite(String query, List<com.librarian.model.entity.Message> history) {
        if (history == null || history.isEmpty()) {
            log.info("No history, returning original query");
            return query;
        }

        log.info("Rewriting query with {} history messages", history.size());

        StringBuilder historyBuilder = new StringBuilder();
        for (com.librarian.model.entity.Message msg : history) {
            String roleLabel = msg.getRole().equals("user") ? "User" : "Assistant";
            historyBuilder.append(roleLabel).append(": ").append(msg.getContent()).append("\n");
        }

        String rewrittenQuery = chatClient.prompt()
                .system("""
                        You are a query rewriter for a RAG system. Given a conversation history and a follow-up question,
                        rewrite the follow-up question as a standalone, self-contained query that can be used for document retrieval.
                        Only output the rewritten query, nothing else.
                        """)
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
