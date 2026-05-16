package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(ContextBuilder.class);

    public String build(List<DocumentChunk> chunks) {
        log.info("Building context from {} chunks", chunks.size());
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            context.append("[来源: ").append(chunk.getMetadataAsString("fileName"))
                   .append("]\n")
                   .append(chunk.getContent())
                   .append("\n\n");
        }
        return context.toString();
    }
}
