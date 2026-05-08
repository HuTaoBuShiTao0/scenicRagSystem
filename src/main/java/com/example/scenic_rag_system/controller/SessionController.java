package com.example.scenic_rag_system.controller;

import com.example.scenic_rag_system.common.Result;
import com.example.scenic_rag_system.service.MessageService;
import com.example.scenic_rag_system.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final MessageService messageService;

    // ============= 用户接口 =============

    @GetMapping("/api/sessions")
    public Result<?> list(HttpServletRequest request) {
        return Result.success(sessionService.list(request));
    }

    @PostMapping("/api/sessions")
    public Result<?> create(HttpServletRequest request) {
        var s = sessionService.create(request);
        return Result.success(Map.of("id", s.getId(), "title", s.getTitle()));
    }

    @DeleteMapping("/api/sessions/{id}")
    public Result<?> delete(@PathVariable Long id, HttpServletRequest request) {
        sessionService.delete(id, request);
        return Result.success();
    }

    @GetMapping("/api/sessions/{id}/messages")
    public Result<?> messages(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(messageService.getMessages(id));
    }

    // ============= 用户分页接口 =============

    @GetMapping("/api/sessions/paged")
    public Result<?> listPaged(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            HttpServletRequest request) {
        try {
            return Result.success(sessionService.listPaged(keyword, page, size, request));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ============= 管理员接口 =============

    @GetMapping("/api/admin/sessions")
    public Result<?> adminList(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        try {
            return Result.success(sessionService.adminListSessions(keyword, page, size));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/api/admin/sessions/{id}/messages")
    public Result<?> adminMessages(@PathVariable Long id) {
        try {
            return Result.success(sessionService.adminGetMessages(id));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }
}
