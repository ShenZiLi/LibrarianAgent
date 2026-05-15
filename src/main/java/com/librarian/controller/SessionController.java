package com.librarian.controller;

import com.librarian.service.ConversationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class SessionController {

    private final ConversationManager conversationManager;

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, String>> createSession() {
        String sessionId = conversationManager.createSession();
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable String sessionId) {
        conversationManager.deleteSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session deleted"));
    }
}
