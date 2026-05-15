package com.librarian.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarian.model.dto.*;
import com.librarian.model.entity.RagTraceLogEntity;
import com.librarian.observability.RagMetrics;
import com.librarian.repository.RagTraceLogRepository;
import com.librarian.security.PiiMasker;
import com.librarian.security.PromptInjectionGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagOrchestrationService {

    private final ConversationManager conversationManager;
    private final QueryRewriterService queryRewriter;
    private final RagRetrievalService retrievalService;
    private final RagGenerationService generationService;
    private final PiiMasker piiMasker;
    private final PromptInjectionGuard injectionGuard;
    private final RagMetrics metrics;
    private final RagTraceLogRepository traceLogRepository;
    private final ObjectMapper objectMapper;

    public AnswerResponse processQuery(QueryRequest request) {
        long totalStartTime = System.currentTimeMillis();
        String traceId = "rag-" + UUID.randomUUID().toString().substring(0, 8);
        metrics.recordRequest();

        log.info("[{}] Processing query: sessionId={}, query={}",
                traceId, request.getSessionId(), piiMasker.mask(request.getQuery()));

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = conversationManager.createSession();
        }

        List<Map<String, Object>> conversationHistory = conversationManager.getMessages(sessionId);

        conversationManager.addMessage(sessionId, "user", request.getQuery());

        String sanitizedQuery = injectionGuard.sanitize(request.getQuery());

        String rewrittenQuery = queryRewriter.rewrite(sanitizedQuery, conversationHistory);

        List<RetrievedContext> contexts = retrievalService.retrieve(
                rewrittenQuery,
                request.getTopK(),
                request.getEnableReranker()
        );

        if (contexts.isEmpty() || contexts.stream().mapToDouble(RetrievedContext::getScore).max().orElse(0.0) < 0.1) {
            log.info("[{}] No relevant context found, returning rejection response", traceId);
            return AnswerResponse.reject("No relevant context found", traceId);
        }

        List<Message> messages = conversationHistory.stream()
                .map(msg -> {
                    String role = (String) msg.get("role");
                    String content = (String) msg.get("content");
                    if ("user".equals(role)) {
                        return new UserMessage(content);
                    } else if ("assistant".equals(role)) {
                        return new AssistantMessage(content);
                    }
                    return null;
                })
                .filter(m -> m != null)
                .collect(Collectors.toList());

        AnswerResponse answerResponse = generationService.generateAnswer(
                sanitizedQuery,
                contexts,
                messages,
                request.getTemperature(),
                traceId
        );

        conversationManager.addMessage(sessionId, "assistant", answerResponse.getAnswer());

        long totalElapsed = System.currentTimeMillis() - totalStartTime;
        answerResponse.setLatencyMs(totalElapsed);
        metrics.recordLatency(totalElapsed);

        logTrace(traceId, sessionId, request.getQuery(), rewrittenQuery, contexts, answerResponse);

        return answerResponse;
    }

    private void logTrace(String traceId, String sessionId, String query, String rewrittenQuery,
                          List<RetrievedContext> contexts, AnswerResponse response) {
        try {
            RagTraceLogEntity logEntity = RagTraceLogEntity.builder()
                    .traceId(traceId)
                    .sessionId(sessionId)
                    .query(piiMasker.mask(query))
                    .rewrittenQuery(piiMasker.mask(rewrittenQuery))
                    .retrievedDocsJson(objectMapper.writeValueAsString(contexts))
                    .generatedAnswer(piiMasker.mask(response.getAnswer()))
                    .citationsJson(objectMapper.writeValueAsString(response.getCitations()))
                    .confidence(response.getConfidence())
                    .totalLatencyMs(response.getLatencyMs())
                    .modelName("glm-4.6v")
                    .temperature(0.1)
                    .rejected(response.getRejected())
                    .rejectReason(response.getRejectReason())
                    .build();

            traceLogRepository.save(logEntity);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize trace log", e);
        }
    }
}
