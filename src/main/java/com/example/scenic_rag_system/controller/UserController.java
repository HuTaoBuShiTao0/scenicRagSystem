package com.example.scenic_rag_system.controller;

import com.example.scenic_rag_system.common.Result;
import com.example.scenic_rag_system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ============= 普通用户接口 =============

    @GetMapping("/api/user/profile")
    public Result<?> getProfile(HttpServletRequest request) {
        try {
            return Result.success(userService.getProfile(request));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/api/user/profile")
    public Result<?> updateProfile(HttpServletRequest request, @RequestBody Map<String, String> body) {
        try {
            return Result.success(userService.updateProfile(request, body.get("nickname"), body.get("avatar")));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ============= 管理员接口 =============

    @GetMapping("/api/admin/users")
    public Result<?> listUsers() {
        try {
            List<Map<String, Object>> users = userService.listAllUsers();
            return Result.success(users);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/api/admin/users/{id}/role")
    public Result<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String role = body.get("role");
            return Result.success(userService.updateUserRole(id, role, request));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }
}
