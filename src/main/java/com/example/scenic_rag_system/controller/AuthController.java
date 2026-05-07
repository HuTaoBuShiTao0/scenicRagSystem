package com.example.scenic_rag_system.controller;

import com.example.scenic_rag_system.common.Result;
import com.example.scenic_rag_system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null || username.isBlank() || password.length() < 3) {
            return Result.error(400, "用户名或密码不合法");
        }
        return authService.register(username.trim(), password);
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return Result.error(400, "请输入用户名和密码");
        }
        return authService.login(username.trim(), password);
    }
}
