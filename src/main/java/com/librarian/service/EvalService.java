package com.librarian.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarian.model.dto.AnswerResponse;
import com.librarian.model.dto.EvalResponse;
import com.librarian.model.dto.QueryRequest;
import com.librarian.model.entity.EvaluationResultEntity;
import com.librarian.repository.EvaluationResultRepository;
import com.librarian.security.PiiMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvalService {

    private final RagOrchestrationService orchestrationService;
    private final EvaluationResultRepository evalResultRepository;
    private final PiiMasker piiMasker;
    private final ObjectMapper objectMapper;

    private final Map<String, List<EvalQuestion>> evalSets = new ConcurrentHashMap<>();

    public EvalResponse runEval(String evalSetName, int sampleSize) {
        long startTime = System.currentTimeMillis();

        List<EvalQuestion> questions = loadEvalSet(evalSetName);
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("Eval set not found: " + evalSetName);
        }

        List<EvalQuestion> sampled = questions.stream()
                .limit(sampleSize)
                .toList();

        List<EvalResponse.EvalDetail> details = new ArrayList<>();
        int correctCount = 0;
        double totalFaithfulness = 0.0;
        double totalContextPrecision = 0.0;
        List<Long> latencies = new ArrayList<>();

        for (EvalQuestion question : sampled) {
            try {
                QueryRequest request = QueryRequest.builder()
                        .query(question.getQuestion())
                        .topK(5)
                        .temperature(0.1)
                        .enableReranker(true)
                        .build();

                AnswerResponse response = orchestrationService.processQuery(request);

                double faithfulness = estimateFaithfulness(question.getExpectedAnswer(), response.getAnswer());
                boolean isCorrect = estimateCorrectness(question.getExpectedAnswer(), response.getAnswer());

                details.add(EvalResponse.EvalDetail.builder()
                        .question(question.getQuestion())
                        .expectedAnswer(question.getExpectedAnswer())
                        .actualAnswer(response.getAnswer())
                        .faithfulnessScore(faithfulness)
                        .isCorrect(isCorrect)
                        .build());

                if (isCorrect) {
                    correctCount++;
                }
                totalFaithfulness += faithfulness;
                totalContextPrecision += (response.getConfidence() != null ? response.getConfidence() : 0.0);
                latencies.add(response.getLatencyMs());

            } catch (Exception e) {
                log.error("Failed to evaluate question: {}", question.getQuestion(), e);
            }
        }

        int n = details.size();
        double accuracy = n > 0 ? (double) correctCount / n : 0.0;
        double avgFaithfulness = n > 0 ? totalFaithfulness / n : 0.0;
        double avgContextPrecision = n > 0 ? totalContextPrecision / n : 0.0;
        long avgLatency = latencies.isEmpty() ? 0 : latencies.stream().mapToLong(Long::longValue).sum() / latencies.size();
        long p90Latency = latencies.isEmpty() ? 0 : calculateP90(latencies);

        long totalElapsed = System.currentTimeMillis() - startTime;
        log.info("Eval completed: accuracy={}, faithfulness={}, context_precision={}, p90={}ms, total_time={}ms",
                accuracy, avgFaithfulness, avgContextPrecision, p90Latency, totalElapsed);

        String evalId = "eval-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            EvaluationResultEntity entity = EvaluationResultEntity.builder()
                    .evalId(evalId)
                    .evalSet(evalSetName)
                    .sampleSize(n)
                    .accuracy(accuracy)
                    .faithfulness(avgFaithfulness)
                    .contextPrecision(avgContextPrecision)
                    .avgLatencyMs(avgLatency)
                    .p90LatencyMs(p90Latency)
                    .detailsJson(objectMapper.writeValueAsString(details))
                    .build();
            evalResultRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to save eval result", e);
        }

        return EvalResponse.builder()
                .accuracy(accuracy)
                .faithfulness(avgFaithfulness)
                .contextPrecision(avgContextPrecision)
                .avgLatencyMs(avgLatency)
                .p90LatencyMs(p90Latency)
                .details(details)
                .build();
    }

    private List<EvalQuestion> loadEvalSet(String evalSetName) {
        return evalSets.computeIfAbsent(evalSetName, this::getDefaultEvalSet);
    }

    private List<EvalQuestion> getDefaultEvalSet(String name) {
        List<EvalQuestion> questions = new ArrayList<>();
        questions.add(new EvalQuestion("年假天数是如何规定的？", "正式员工根据工龄享有5-15天年假"));
        questions.add(new EvalQuestion("报销流程是什么？", "报销流程包括提交申请、部门审批、财务审核、打款"));
        questions.add(new EvalQuestion("公司的工作时间是怎样的？", "标准工作时间为周一至周五，上午9:00至下午6:00"));
        questions.add(new EvalQuestion("试用期是多久？", "试用期一般为3个月"));
        questions.add(new EvalQuestion("如何申请病假？", "病假需提供医院证明，经部门主管审批后提交HR"));
        return questions;
    }

    private double estimateFaithfulness(String expected, String actual) {
        if (actual == null || actual.isEmpty()) {
            return 0.0;
        }
        String[] expectedWords = expected.split("\\s+|(?<!\u4e00)(?!\u4e00)");
        int matchCount = 0;
        for (String word : expectedWords) {
            if (word.length() > 1 && actual.contains(word)) {
                matchCount++;
            }
        }
        return expectedWords.length > 0 ? (double) matchCount / expectedWords.length : 0.0;
    }

    private boolean estimateCorrectness(String expected, String actual) {
        if (actual == null || actual.isEmpty()) {
            return false;
        }
        String[] keyTerms = expected.length() > 4
                ? new String[]{expected.substring(0, 4), expected.substring(expected.length() / 2, Math.min(expected.length() / 2 + 4, expected.length()))}
                : new String[]{expected};

        for (String term : keyTerms) {
            if (term.length() > 1 && actual.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private long calculateP90(List<Long> latencies) {
        if (latencies.isEmpty()) {
            return 0;
        }
        latencies.sort(Long::compareTo);
        int index = (int) Math.ceil(latencies.size() * 0.9) - 1;
        return latencies.get(Math.max(0, index));
    }

    private record EvalQuestion(String question, String expectedAnswer) {
    }
}
