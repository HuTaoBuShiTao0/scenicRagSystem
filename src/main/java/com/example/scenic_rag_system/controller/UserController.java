package com.example.scenic_rag_system.controller;

import com.example.scenic_rag_system.common.Result;
import com.example.scenic_rag_system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<?> getProfile(HttpServletRequest request) {
        try {
            return Result.success(userService.getProfile(request));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(HttpServletRequest request, @RequestBody Map<String, String> body) {
        try {
            return Result.success(userService.updateProfile(request, body.get("nickname"), body.get("avatar")));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }
}
