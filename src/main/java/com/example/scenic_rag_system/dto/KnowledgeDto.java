package com.example.scenic_rag_system.dto;

import lombok.Data;

import java.util.Map;

@Data
public class KnowledgeDto {
    /** 知识库类型 */
    private String type;

    /** 数据内容（字段名 -> 值） */
    private Map<String, Object> data;
}
