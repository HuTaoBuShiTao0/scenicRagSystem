package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.entity.Message;
import com.example.scenic_rag_system.entity.Session;
import com.example.scenic_rag_system.entity.User;
import com.example.scenic_rag_system.repository.MessageRepository;
import com.example.scenic_rag_system.repository.SessionRepository;
import com.example.scenic_rag_system.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    // ============= 用户接口 =============

    public List<Map<String, Object>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return sessionRepository.findByUserIdWithMessages(userId).stream()
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

    /**
     * 分页查询当前用户的会话，支持按标题/消息内容搜索
     */
    public Map<String, Object> listPaged(String keyword, int page, int size, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Session> sessionPage = sessionRepository.searchUserSessions(userId, keyword, pageable);

        List<Long> sessionIds = sessionPage.getContent().stream()
                .map(Session::getId).collect(Collectors.toList());
        Map<Long, Long> msgCountMap = getMessageCountBySessionIds(sessionIds);

        List<Map<String, Object>> list = sessionPage.getContent().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("title", s.getTitle() != null ? s.getTitle() : "新对话");
            m.put("messageCount", msgCountMap.getOrDefault(s.getId(), 0L));
            m.put("createdAt", s.getCreatedAt());
            m.put("updatedAt", s.getUpdatedAt());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", list);
        result.put("page", sessionPage.getNumber());
        result.put("size", sessionPage.getSize());
        result.put("totalElements", sessionPage.getTotalElements());
        result.put("totalPages", sessionPage.getTotalPages());
        return result;
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

    // ============= 管理员接口 =============

    /**
     * 管理员分页查询所有会话，支持按用户名/昵称/消息内容搜索
     */
    public Map<String, Object> adminListSessions(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Session> sessionPage = sessionRepository.adminSearchSessions(keyword, pageable);

        // 收集所有用户 ID
        Set<Long> userIds = sessionPage.getContent().stream()
                .map(Session::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 收集所有会话 ID 并获取消息数
        List<Long> sessionIds = sessionPage.getContent().stream()
                .map(Session::getId).collect(Collectors.toList());
        Map<Long, Long> msgCountMap = getMessageCountBySessionIds(sessionIds);

        List<Map<String, Object>> list = sessionPage.getContent().stream().map(s -> {
            User u = userMap.get(s.getUserId());
            long msgCount = msgCountMap.getOrDefault(s.getId(), 0L);
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("title", s.getTitle() != null ? s.getTitle() : "新对话");
            m.put("userId", s.getUserId());
            m.put("username", u != null ? u.getUsername() : "未知用户");
            m.put("nickname", u != null ? (u.getNickname() != null ? u.getNickname() : u.getUsername()) : "未知用户");
            m.put("messageCount", msgCount);
            m.put("createdAt", s.getCreatedAt());
            m.put("updatedAt", s.getUpdatedAt());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", list);
        result.put("page", sessionPage.getNumber());
        result.put("size", sessionPage.getSize());
        result.put("totalElements", sessionPage.getTotalElements());
        result.put("totalPages", sessionPage.getTotalPages());
        return result;
    }

    /**
     * 管理员获取指定会话的消息（读权限校验）
     */
    public List<Map<String, Object>> adminGetMessages(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getId());
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private Map<Long, Long> getMessageCountBySessionIds(List<Long> sessionIds) {
        if (sessionIds.isEmpty()) return Map.of();
        List<Object[]> counts = messageRepository.countBySessionIds(sessionIds);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : counts) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }
}
