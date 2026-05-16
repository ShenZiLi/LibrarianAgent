package com.librarian.controller;

import com.librarian.model.dto.ChatDto.*;
import com.librarian.service.ChatService;
import com.librarian.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/sessions")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public SessionResponse createSession(@RequestBody(required = false) CreateSessionRequest request) {
        LoggerUtil.setRequestId();
        log.info("Creating new chat session");
        SessionResponse response = chatService.createSession(request);
        log.info("Session created: {}", response.sessionId());
        return response;
    }

    @GetMapping
    public List<SessionResponse> listSessions() {
        LoggerUtil.setRequestId();
        log.info("Listing all sessions");
        return chatService.listSessions();
    }

    @GetMapping("/{sessionId}")
    public SessionWithMessages getSession(@PathVariable String sessionId) {
        LoggerUtil.setRequestId();
        log.info("Getting session: {}", sessionId);
        return chatService.getSession(sessionId);
    }

    @PostMapping("/{sessionId}/message")
    public MessageResponse sendMessage(@PathVariable String sessionId, 
                                       @RequestBody MessageRequest request) {
        LoggerUtil.setRequestId();
        log.info("Sending message to session: {}", sessionId);
        return chatService.sendMessage(sessionId, request);
    }

    @PostMapping(value = "/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@PathVariable String sessionId,
                                    @RequestBody MessageRequest request) {
        LoggerUtil.setRequestId();
        log.info("Starting stream for session: {}", sessionId);
        return chatService.streamMessage(sessionId, request);
    }
}
