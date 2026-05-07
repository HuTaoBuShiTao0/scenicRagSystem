package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.entity.Message;
import com.example.scenic_rag_system.repository.MessageRepository;
import com.example.scenic_rag_system.repository.SessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final SessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> getMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    if (m.getCardData() != null) {
                        try {
                            map.put("card", objectMapper.readValue(m.getCardData(), Map.class));
                        } catch (Exception e) { /* ignore */ }
                    }
                    return map;
                })
                .toList();
    }

    public void addMessage(Long sessionId, String role, String content, Object card) {
        Message m = new Message();
        m.setSessionId(sessionId);
        m.setRole(role);
        m.setContent(content);
        if (card != null) {
            try {
                m.setCardData(objectMapper.writeValueAsString(card));
            } catch (Exception e) { /* ignore */ }
        }
        messageRepository.save(m);

        // 更新会话时间
        sessionRepository.findById(sessionId).ifPresent(s -> {
            s.setUpdatedAt(java.time.LocalDateTime.now());
            sessionRepository.save(s);
        });
    }
}
