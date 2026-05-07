package com.example.scenic_rag_system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * 轻量级本地嵌入生成器
 * 使用基于CRC32哈希的词袋方法产生384维向量
 * 作为智谱嵌入API不可用时的替代方案
 */
@Service
@Slf4j
public class LocalEmbeddingService {

    private static final int VECTOR_DIM = 384;
    private static final int SEED_COUNT = 384;

    private final List<Long> seeds;

    public LocalEmbeddingService() {
        // 生成384个随机种子
        seeds = new ArrayList<>(SEED_COUNT);
        CRC32 crc32 = new CRC32();
        for (int i = 0; i < SEED_COUNT; i++) {
            crc32.update((i * 31 + 7) & 0xFF);
            crc32.update((i * 17 + 13) & 0xFF);
            crc32.update((i * 53 + 29) & 0xFF);
            seeds.add(crc32.getValue());
            crc32.reset();
        }
    }

    /**
     * 将文本转为384维向量
     */
    public List<Double> embed(String text) {
        if (text == null || text.isBlank()) {
            return zeroVector();
        }

        // 分词
        String[] tokens = tokenize(text);
        if (tokens.length == 0) {
            return zeroVector();
        }

        double[] vector = new double[VECTOR_DIM];
        CRC32 crc32 = new CRC32();

        // 对每个token进行哈希并累加到向量
        for (String token : tokens) {
            byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);

            for (int dim = 0; dim < VECTOR_DIM; dim++) {
                crc32.update(tokenBytes);
                crc32.update((dim) & 0xFF);
                crc32.update((dim >> 8) & 0xFF);
                long hash = crc32.getValue();
                crc32.reset();

                // 用hash的低位决定符号，高位决定幅值
                double sign = (hash & 1) == 0 ? 1.0 : -1.0;
                double magnitude = (hash >> 1) & 0x7FFF;
                vector[dim] += sign * (magnitude / 32767.0);
            }
        }

        // L2归一化
        double norm = 0.0;
        for (double v : vector) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < VECTOR_DIM; i++) {
                vector[i] /= norm;
            }
        }

        List<Double> result = new ArrayList<>(VECTOR_DIM);
        for (double v : vector) result.add(v);
        return result;
    }

    /**
     * 为RAG检索构建嵌入文本
     */
    public List<Double> embedRagText(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (String field : fields) {
            if (field != null && !field.isBlank()) {
                sb.append(field).append(" ");
            }
        }
        return embed(sb.toString().trim());
    }

    private List<Double> zeroVector() {
        List<Double> vec = new ArrayList<>(VECTOR_DIM);
        for (int i = 0; i < VECTOR_DIM; i++) vec.add(0.0);
        return vec;
    }

    /**
     * 简单的分词：按非中文字符和非字母数字拆分
     */
    private String[] tokenize(String text) {
        // 对中文：按字拆分；对英文：按空格/标点拆分
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isIdeographic(c)) {
                // 中文字符：单个字作为一个token
                if (current.length() > 0) {
                    tokens.add(current.toString().toLowerCase());
                    current = new StringBuilder();
                }
                tokens.add(String.valueOf(c));
            } else if (Character.isLetterOrDigit(c)) {
                current.append(c);
            } else {
                if (current.length() > 0) {
                    tokens.add(current.toString().toLowerCase());
                    current = new StringBuilder();
                }
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString().toLowerCase());
        }

        return tokens.toArray(new String[0]);
    }
}
