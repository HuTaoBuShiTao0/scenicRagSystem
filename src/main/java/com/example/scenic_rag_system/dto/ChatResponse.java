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

    /** 卡片数据（非流式返回时携带） */
    private CardData card;

    /** 意图类型 */
    private String intentType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardData {
        private String type;       // scenic, food, hotel, product, ticket
        private Map<String, Object> data;
    }
}
