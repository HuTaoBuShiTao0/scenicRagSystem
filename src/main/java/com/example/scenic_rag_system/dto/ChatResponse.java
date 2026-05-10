package com.example.scenic_rag_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    /** 文本内容（流式返回时逐段累加） */
    private String content;

    /** 卡片数据（单条，兼容旧版） */
    private CardData card;

    /** 多条卡片数据（按相关性排序，流式结束后统一返回） */
    private List<CardData> cards;

    /** 意图类型 */
    private String intentType;

    /** 会话 ID（新建会话时返回） */
    private Long sessionId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardData {
        private String type;       // scenic, food, hotel, product, ticket
        private Map<String, Object> data;
    }
}
