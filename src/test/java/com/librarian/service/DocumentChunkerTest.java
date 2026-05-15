package com.librarian.service;

import com.librarian.config.RagProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentChunkerTest {

    private final RagProperties ragProperties;
    private final DocumentChunker chunker;

    DocumentChunkerTest() {
        ragProperties = new RagProperties();
        ragProperties.getChunk().setTargetSize(512);
        ragProperties.getChunk().setOverlap(128);
        ragProperties.getChunk().setMinSize(256);
        ragProperties.getChunk().setMaxSize(768);
        chunker = new DocumentChunker(ragProperties);
    }

    @Test
    void shouldChunkSimpleChineseText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("这是第").append(i + 1).append("条内容，用于测试分块功能是否正常工作。");
        }
        String text = sb.toString();

        List<String> chunks = chunker.chunk(text, "zh");

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 2);
    }

    @Test
    void shouldChunkSimpleEnglishText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("This is sentence number ").append(i + 1).append(". ");
        }
        String text = sb.toString();

        List<String> chunks = chunker.chunk(text, "en");

        assertFalse(chunks.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullOrEmptyInput() {
        assertTrue(chunker.chunk(null, "zh").isEmpty());
        assertTrue(chunker.chunk("", "zh").isEmpty());
    }

    @Test
    void shouldRespectMinimumChunkSize() {
        String shortText = "这是一段很短的文本。";
        List<String> chunks = chunker.chunk(shortText, "zh");
        assertTrue(chunks.isEmpty());
    }
}
