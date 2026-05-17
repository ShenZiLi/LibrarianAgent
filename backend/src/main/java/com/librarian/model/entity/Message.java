package com.librarian.model.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Message {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";

    private String role;
    private String content;
    private Instant timestamp;
    private List<String> citations;

    public Message() {
        this.timestamp = Instant.now();
        this.citations = new ArrayList<>();
    }

    public Message(String role, String content) {
        this();
        this.role = role;
        this.content = content;
    }

    public Message(String role, String content, List<String> citations) {
        this(role, content);
        this.citations = citations != null ? citations : new ArrayList<>();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public List<String> getCitations() {
        return citations;
    }

    public void setCitations(List<String> citations) {
        this.citations = citations;
    }
}
