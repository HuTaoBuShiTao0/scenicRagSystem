package com.example.scenic_rag_system.controller;

import com.example.scenic_rag_system.common.Result;
import com.example.scenic_rag_system.service.MessageService;
import com.example.scenic_rag_system.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final MessageService messageService;

    @GetMapping
    public Result<?> list(HttpServletRequest request) {
        return Result.success(sessionService.list(request));
    }

    @PostMapping
    public Result<?> create(HttpServletRequest request) {
        var s = sessionService.create(request);
        return Result.success(Map.of("id", s.getId(), "title", s.getTitle()));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id, HttpServletRequest request) {
        sessionService.delete(id, request);
        return Result.success();
    }

    @GetMapping("/{id}/messages")
    public Result<?> messages(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(messageService.getMessages(id));
    }
}
