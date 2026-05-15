package com.librarian.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RagMetrics {

    private final Counter requestCounter;
    private final Timer latencyTimer;
    private final Counter llmCallCounter;
    private final Counter inputTokenCounter;
    private final Counter outputTokenCounter;

    public RagMetrics(MeterRegistry meterRegistry) {
        this.requestCounter = Counter.builder("rag.requests.total")
                .description("Total RAG requests")
                .register(meterRegistry);

        this.latencyTimer = Timer.builder("rag.requests.latency")
                .description("RAG request latency")
                .register(meterRegistry);

        this.llmCallCounter = Counter.builder("llm.calls.total")
                .description("Total LLM calls")
                .register(meterRegistry);

        this.inputTokenCounter = Counter.builder("llm.tokens.input.total")
                .description("Total input tokens")
                .register(meterRegistry);

        this.outputTokenCounter = Counter.builder("llm.tokens.output.total")
                .description("Total output tokens")
                .register(meterRegistry);
    }

    public void recordRequest() {
        requestCounter.increment();
    }

    public void recordLatency(long milliseconds) {
        latencyTimer.record(java.time.Duration.ofMillis(milliseconds));
    }

    public void recordLlmCall(int inputTokens, int outputTokens) {
        llmCallCounter.increment();
        inputTokenCounter.increment(inputTokens);
        outputTokenCounter.increment(outputTokens);
    }
}
