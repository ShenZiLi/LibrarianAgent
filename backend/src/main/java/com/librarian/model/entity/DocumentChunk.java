package com.librarian.model.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DocumentChunk {

    private final String chunkId;
    private String documentId;
    private String content;
    private Map<String, Object> metadata;

    public DocumentChunk() {
        this.chunkId = UUID.randomUUID().toString();
        this.metadata = new HashMap<>();
    }

    public DocumentChunk(String documentId, String content) {
        this();
        this.documentId = documentId;
        this.content = content;
    }

    public String getChunkId() {
        return chunkId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public String getMetadataAsString(String key) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : null;
    }

    public double getMetadataAsDouble(String key, double defaultValue) {
        Object value = metadata.get(key);
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return defaultValue;
    }
}
