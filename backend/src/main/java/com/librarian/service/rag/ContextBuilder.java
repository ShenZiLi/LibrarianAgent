package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
public class ContextBuilder {

    public String build(List<DocumentChunk> chunks) {
        log.info("Building context from {} chunks", chunks.size());

        if (chunks.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        IntStream.range(0, chunks.size()).forEach(i -> {
            DocumentChunk chunk = chunks.get(i);
            int index = i + 1;
            String fileName = chunk.getMetadataAsString("fileName");
            String sourceLabel = fileName != null ? fileName : "Unknown";

            context.append("[来源").append(index).append(": ").append(sourceLabel).append("]\n");
            context.append(chunk.getContent()).append("\n\n");
        });

        return context.toString();
    }

    public List<String> buildCitations(List<DocumentChunk> chunks) {
        return chunks.stream()
                .map(chunk -> {
                    String fileName = chunk.getMetadataAsString("fileName");
                    return fileName != null ? fileName : "Unknown";
                })
                .toList();
    }
}
