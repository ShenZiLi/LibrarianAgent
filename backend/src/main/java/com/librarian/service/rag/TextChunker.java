package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class TextChunker {

    private static final Logger log = LoggerFactory.getLogger(TextChunker.class);

    @Value("${rag.chunk-size}")
    private int chunkSize;

    @Value("${rag.chunk-overlap}")
    private int chunkOverlap;

    public List<DocumentChunk> chunk(DocumentChunk document) {
        log.info("Chunking document: {}", document.getChunkId());
        return Collections.singletonList(document);
    }

    public List<DocumentChunk> chunkAll(List<DocumentChunk> documents) {
        return documents.stream()
                .map(this::chunk)
                .flatMap(List::stream)
                .toList();
    }
}
