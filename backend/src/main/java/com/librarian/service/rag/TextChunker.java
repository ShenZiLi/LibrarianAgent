package com.librarian.service.rag;

import com.librarian.model.entity.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TextChunker {

    @Value("${rag.chunk-size}")
    private int chunkSize;

    @Value("${rag.chunk-overlap}")
    private int chunkOverlap;

    public List<DocumentChunk> chunk(DocumentChunk document) {
        log.info("Chunking document: {} (size={}, overlap={})", document.getChunkId(), chunkSize, chunkOverlap);
        
        String content = document.getContent();
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        List<String> paragraphs = splitByParagraphs(content);
        List<DocumentChunk> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        List<String> overlapBuffer = new ArrayList<>();

        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > chunkSize * 4 && !currentChunk.isEmpty()) {
                DocumentChunk chunk = createChunk(document, currentChunk.toString());
                chunks.add(chunk);

                overlapBuffer.clear();
                String overlapText = currentChunk.toString();
                int overlapStart = Math.max(0, overlapText.length() - chunkOverlap * 4);
                if (overlapStart > 0) {
                    overlapBuffer.add(overlapText.substring(overlapStart));
                }

                currentChunk = new StringBuilder();
                overlapBuffer.forEach(currentChunk::append);
            }

            currentChunk.append(paragraph).append("\n\n");
        }

        if (!currentChunk.isEmpty()) {
            DocumentChunk chunk = createChunk(document, currentChunk.toString());
            chunks.add(chunk);
        }

        if (chunks.isEmpty()) {
            chunks.add(createChunk(document, content));
        }

        log.info("Created {} chunks from document {}", chunks.size(), document.getChunkId());
        return chunks;
    }

    public List<DocumentChunk> chunkAll(List<DocumentChunk> documents) {
        return documents.stream()
                .map(this::chunk)
                .flatMap(List::stream)
                .toList();
    }

    private List<String> splitByParagraphs(String content) {
        String[] paragraphs = content.split("\\n\\s*\\n");
        List<String> result = new ArrayList<>();
        for (String p : paragraphs) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private DocumentChunk createChunk(DocumentChunk source, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(source.getDocumentId());
        chunk.setContent(content.trim());
        source.getMetadata().forEach(chunk::addMetadata);
        return chunk;
    }
}
