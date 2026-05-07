package com.example.scenic_rag_system.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {
    /** 用户消息 */
    private String message;

    /** 对话历史 */
    private List<ChatMessage> history;

    /** 会话ID（传null则新建会话） */
    private Long sessionId;

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
