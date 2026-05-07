package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.entity.Message;
import com.example.scenic_rag_system.entity.Session;
import com.example.scenic_rag_system.repository.MessageRepository;
import com.example.scenic_rag_system.repository.SessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;

    public List<Map<String, Object>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", s.getId());
                    m.put("title", s.getTitle() != null ? s.getTitle() : "新对话");
                    m.put("createdAt", s.getCreatedAt());
                    m.put("updatedAt", s.getUpdatedAt());
                    return m;
                })
                .toList();
    }

    public Session create(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return createByUserId(userId);
    }

    public Session createByUserId(Long userId) {
        Session s = new Session();
        s.setUserId(userId);
        s.setTitle("新对话");
        return sessionRepository.save(s);
    }

    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Session s = sessionRepository.findById(id).orElse(null);
        if (s == null || !s.getUserId().equals(userId)) return;
        messageRepository.deleteBySessionId(id);
        sessionRepository.deleteById(id);
    }

    public void updateTitle(Long sessionId, String title) {
        sessionRepository.findById(sessionId).ifPresent(s -> {
            s.setTitle(title);
            sessionRepository.save(s);
        });
    }
}
