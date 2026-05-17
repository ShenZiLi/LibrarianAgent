package com.librarian.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

public class LoggerUtil {

    private static final String REQUEST_ID_KEY = "requestId";

    public static String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static void setRequestId() {
        MDC.put(REQUEST_ID_KEY, generateRequestId());
    }

    public static void clearRequestId() {
        MDC.remove(REQUEST_ID_KEY);
    }

    public static String getRequestId() {
        return MDC.get(REQUEST_ID_KEY);
    }

    public static void logChatMetrics(Logger log, long retrievalTimeMs, 
                                      long generationTimeMs, int retrievedDocs, 
                                      double avgSimilarity) {
        log.info("Chat completed - retrieval_time_ms={}, generation_time_ms={}, " +
                        "total_time_ms={}, retrieved_docs={}, avg_similarity={}",
                retrievalTimeMs, generationTimeMs, 
                retrievalTimeMs + generationTimeMs,
                retrievedDocs, String.format("%.2f", avgSimilarity));
    }
}
