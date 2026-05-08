package com.example.scenic_rag_system.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role) && !"OWNER".equals(role)) {
            throw new RuntimeException("权限不足，需要管理员权限");
        }
        return true;
    }
}
