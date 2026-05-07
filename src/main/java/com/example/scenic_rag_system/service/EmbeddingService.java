package com.example.scenic_rag_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文本向量化服务
 * 使用本地嵌入生成器（因为智谱嵌入API在该账号下无配额）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    private final LocalEmbeddingService localEmbeddingService;

    public List<Double> embed(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return localEmbeddingService.embed(text);
    }

    public List<Double> embedRagText(String... fields) {
        return localEmbeddingService.embedRagText(fields);
    }
}
