package com.example.scenic_rag_system.interceptor;

import com.example.scenic_rag_system.common.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new RuntimeException("未登录或token已过期");
        }

        String token = auth.substring(7);
        Claims claims = jwtUtil.parse(token);
        request.setAttribute("userId", claims.get("userId", Integer.class).longValue());
        request.setAttribute("username", claims.get("username", String.class));
        request.setAttribute("role", claims.get("role", String.class));
        return true;
    }
}
