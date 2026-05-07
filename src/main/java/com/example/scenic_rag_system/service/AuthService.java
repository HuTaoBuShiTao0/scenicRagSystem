package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.common.JwtUtil;
import com.example.scenic_rag_system.common.Result;
import com.example.scenic_rag_system.entity.User;
import com.example.scenic_rag_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    private String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes());
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash error");
        }
    }

    public Result<?> register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            return Result.error(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(hash(password));
        user.setNickname(username);
        user.setRole("USER");
        userRepository.save(user);
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return Result.success(Map.of("token", token, "user", toMap(user)));
    }

    public Result<?> login(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !user.getPassword().equals(hash(password))) {
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
