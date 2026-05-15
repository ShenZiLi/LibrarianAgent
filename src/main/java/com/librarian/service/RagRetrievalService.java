package com.librarian.service;

import com.librarian.config.RagProperties;
import com.librarian.model.dto.RetrievedContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private final VectorStoreService vectorStoreService;
    private final RagProperties ragProperties;
    private final RestTemplate restTemplate;

    public List<RetrievedContext> retrieve(String query, int topK, boolean enableReranker) {
        long startTime = System.currentTimeMillis();

        int searchTopK = enableReranker ? Math.max(topK * 2, 10) : topK;

        List<RetrievedContext> results = vectorStoreService.search(query, searchTopK, null);

        if (results.isEmpty()) {
            log.debug("No results retrieved for query: {}", query);
            return Collections.emptyList();
        }

        if (enableReranker && ragProperties.getReranker().isEnabled()) {
            results = rerank(query, results);
        }

        List<RetrievedContext> finalResults = results.stream()
                .filter(ctx -> ctx.getScore() >= ragProperties.getSimilarityThreshold())
                .limit(topK)
                .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("Retrieval pipeline completed in {}ms, returned {} results (threshold={})",
                elapsed, finalResults.size(), ragProperties.getSimilarityThreshold());

        return finalResults;
    }

    private List<RetrievedContext> rerank(String query, List<RetrievedContext> candidates) {
        long startTime = System.currentTimeMillis();

        try {
            String rerankerUrl = ragProperties.getReranker().getUrl();

            List<Map<String, String>> passages = candidates.stream()
                    .map(ctx -> Map.of("text", ctx.getContent()))
                    .collect(Collectors.toList());

            Map<String, Object> requestBody = Map.of(
                    "query", query,
                    "passages", passages
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    rerankerUrl,
                    requestBody,
                    Map.class
            );

            if (response != null && response.containsKey("scores")) {
                @SuppressWarnings("unchecked")
                List<Double> scores = (List<Double>) response.get("scores");

                for (int i = 0; i < Math.min(scores.size(), candidates.size()); i++) {
                    candidates.get(i).setScore(scores.get(i));
                }

                candidates.sort(Comparator.comparingDouble(RetrievedContext::getScore).reversed());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            int topN = ragProperties.getReranker().getTopN();
            return candidates.stream().limit(topN).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Reranker service unavailable at {}, falling back to vector scores",
                    ragProperties.getReranker().getUrl(), e);
            return candidates.stream()
                    .limit(ragProperties.getReranker().getTopN())
                    .collect(Collectors.toList());
        }
    }
}
