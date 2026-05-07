package com.example.scenic_rag_system.controller;

import com.example.scenic_rag_system.dto.ChatRequest;
import com.example.scenic_rag_system.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        final String msg = request.getMessage();
        final Long sessionId = request.getSessionId();
        final Long userId = (Long) httpRequest.getAttribute("userId");

        final SseEmitter emitter = new SseEmitter(120000L);

        new Thread(() -> {
            try {
                chatService.processMessageStream(msg, sessionId, userId, emitter, objectMapper);
            } catch (Exception e) {
                log.error("Chat error: {}", e.getMessage());
                try { emitter.send(SseEmitter.event().name("error").data(e.getMessage())); } catch (Exception ignored) {}
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            }
        }).start();

        return emitter;
    }
}
