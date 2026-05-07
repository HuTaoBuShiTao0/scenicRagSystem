package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.common.JwtUtil;
import com.example.scenic_rag_system.common.Result;
import com.example.scenic_rag_system.entity.User;
import com.example.scenic_rag_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Result<?> register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            return Result.error(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setNickname(username);
        user.setRole("USER");
        userRepository.save(user);
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return Result.success(Map.of("token", token, "user", toMap(user)));
    }

    public Result<?> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElse(null);
        if (user == null || !encoder.matches(password, user.getPassword())) {
            return Result.error(400, "用户名或密码错误");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return Result.success(Map.of("token", token, "user", toMap(user)));
    }

    private Map<String, Object> toMap(User u) {
        return Map.of(
            "id", u.getId(),
            "username", u.getUsername(),
            "nickname", u.getNickname() != null ? u.getNickname() : u.getUsername(),
            "avatar", u.getAvatar() != null ? u.getAvatar() : "",
            "role", u.getRole()
        );
    }
}
