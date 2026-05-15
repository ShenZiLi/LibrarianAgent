package com.librarian.service;

import com.librarian.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentChunker {

    private final RagProperties ragProperties;

    private static final Pattern CHINESE_SENTENCE_BOUNDARY = Pattern.compile("[。！？；]");
    private static final Pattern ENGLISH_SENTENCE_BOUNDARY = Pattern.compile("[.!?;]");
    private static final Pattern PARAGRAPH_BOUNDARY = Pattern.compile("\\n\\s*\\n");

    public List<String> chunk(String text, String language) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int targetSize = ragProperties.getChunk().getTargetSize();
        int overlap = ragProperties.getChunk().getOverlap();

        String[] paragraphs = PARAGRAPH_BOUNDARY.split(text);
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmedParagraph = paragraph.trim();
            if (trimmedParagraph.isEmpty()) {
                continue;
            }

            int paragraphTokenCount = estimateTokenCount(trimmedParagraph);

            if (currentChunk.length() > 0 &&
                    estimateTokenCount(currentChunk.toString()) + paragraphTokenCount > targetSize) {

                if (currentChunk.length() > ragProperties.getChunk().getMinSize()) {
                    chunks.add(currentChunk.toString().trim());
                }

                if (paragraphTokenCount > targetSize) {
                    List<String> subChunks = splitLargeParagraph(trimmedParagraph, targetSize, overlap, language);
                    chunks.addAll(subChunks);
                    currentChunk = new StringBuilder();
                } else {
                    String overlapText = getOverlapText(currentChunk.toString(), overlap);
                    currentChunk = new StringBuilder(overlapText);
                    currentChunk.append(trimmedParagraph);
                }
            } else {
                currentChunk.append(trimmedParagraph);
            }
        }

        if (currentChunk.length() > 0 && currentChunk.length() >= ragProperties.getChunk().getMinSize()) {
            chunks.add(currentChunk.toString().trim());
        }

        log.debug("Chunked text into {} chunks (target={}, overlap={})",
                chunks.size(), targetSize, overlap);

        return chunks;
    }

    private List<String> splitLargeParagraph(String text, int targetSize, int overlap, String language) {
        List<String> subChunks = new ArrayList<>();
        Pattern boundary = "zh".equals(language) ? CHINESE_SENTENCE_BOUNDARY : ENGLISH_SENTENCE_BOUNDARY;
        String[] sentences = boundary.split(text);

        StringBuilder currentSubChunk = new StringBuilder();
        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (trimmedSentence.isEmpty()) {
                continue;
            }

            if (estimateTokenCount(currentSubChunk.toString()) + estimateTokenCount(trimmedSentence) > targetSize) {
                if (currentSubChunk.length() > 0) {
                    subChunks.add(currentSubChunk.toString().trim());
                }
                currentSubChunk = new StringBuilder(getOverlapText(currentSubChunk.toString(), overlap));
            }
            currentSubChunk.append(trimmedSentence);
        }

        if (currentSubChunk.length() > ragProperties.getChunk().getMinSize()) {
            subChunks.add(currentSubChunk.toString().trim());
        }

        return subChunks;
    }

    private String getOverlapText(String text, int overlap) {
        if (text.isEmpty() || overlap <= 0) {
            return "";
        }
        int charsToTake = Math.min(text.length(), overlap * 2);
        int startIndex = Math.max(0, text.length() - charsToTake);
        return text.substring(startIndex) + "\n";
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
