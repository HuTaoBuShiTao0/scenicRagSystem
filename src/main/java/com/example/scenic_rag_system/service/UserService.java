package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.entity.User;
import com.example.scenic_rag_system.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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

    // ============= 初始化 =============

    /**
     * 启动时确保 Owner 账号存在且密码正确
     */
    public void initOwner() {
        User owner = userRepository.findByUsername("owner").orElse(null);
        if (owner == null) {
            owner = new User();
            owner.setUsername("owner");
            owner.setNickname("系统管理员");
            owner.setRole("OWNER");
            owner.setPassword(hash("owner123"));
            userRepository.save(owner);
            log.info("Owner 账号已创建（用户名: owner, 密码: owner123）");
        } else {
            // 每次启动都刷新密码，确保哈希一致性
            owner.setPassword(hash("owner123"));
            userRepository.save(owner);
            log.info("Owner 账号密码已刷新");
        }
    }

    private String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes());
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash error");
        }
    }

    // ============= 管理员：用户管理 =============

    public List<Map<String, Object>> listAllUsers() {
        return userRepository.findAllByOrderByIdAsc().stream()
            .map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "username", u.getUsername(),
                "nickname", u.getNickname() != null ? u.getNickname() : u.getUsername(),
                "avatar", u.getAvatar() != null ? u.getAvatar() : "",
                "role", u.getRole(),
                "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
            ))
            .collect(Collectors.toList());
    }

    public Map<String, Object> updateUserRole(Long id, String newRole, HttpServletRequest request) {
        User currentUser = getCurrentUser(request).orElseThrow(() -> new RuntimeException("未登录"));
        if (currentUser.getId().equals(id)) {
            throw new RuntimeException("不能修改自己的权限");
        }
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        if ("OWNER".equals(user.getRole())) {
            throw new RuntimeException("无法修改 Owner 账号的权限");
        }
        if (!List.of("USER", "ADMIN").contains(newRole)) {
            throw new RuntimeException("无效的角色: " + newRole);
        }
        user.setRole(newRole);
        userRepository.save(user);
        return Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername(),
            "role", user.getRole()
        );
    }
}
