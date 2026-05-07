package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.entity.User;
import com.example.scenic_rag_system.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userRepository.findById(userId);
    }

    public Map<String, Object> getProfile(HttpServletRequest request) {
        User user = getCurrentUser(request).orElseThrow(() -> new RuntimeException("用户不存在"));
        return Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername(),
            "avatar", user.getAvatar() != null ? user.getAvatar() : "",
            "role", user.getRole()
        );
    }

    public Map<String, Object> updateProfile(HttpServletRequest request, String nickname, String avatar) {
        User user = getCurrentUser(request).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (nickname != null) user.setNickname(nickname);
        if (avatar != null) user.setAvatar(avatar);
        userRepository.save(user);
        return Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "nickname", user.getNickname(),
            "avatar", user.getAvatar() != null ? user.getAvatar() : "",
            "role", user.getRole()
        );
    }
}
