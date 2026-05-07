package com.example.scenic_rag_system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.*;

@Service
@Slf4j
public class ZhipuApiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ZhipuApiService(@Qualifier("zhipuWebClient") WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Value("${zhipu.api-key}")
    private String apiKey;

    @Value("${zhipu.model}")
    private String model;

    @Value("${zhipu.embedding-model}")
    private String embeddingModel;

    /**
     * 智谱API流式对话
     * 使用HttpURLConnection逐行读取SSE，避免WebClient的chunk切割问题
     */
    public Flux<String> chatStream(List<Map<String, String>> messages, String userMessage) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);

        ArrayNode messagesArray = requestBody.putArray("messages");
        for (Map<String, String> msg : messages) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.get("role"));
            msgNode.put("content", msg.get("content"));
        }

        requestBody.put("stream", true);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2048);

        String bodyStr = requestBody.toString();

        return Flux.create(emitter -> {
            try {
                java.net.URL url = java.net.URI.create("https://open.bigmodel.cn/api/paas/v4/chat/completions").toURL();
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(120000);

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(bodyStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }

                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) continue;
                            try {
                                JsonNode node = objectMapper.readTree(data);
                                String text = node.path("choices").get(0)
                                        .path("delta").path("content").asText("");
                                if (!text.isEmpty()) {
                                    emitter.next(text);
                                }
                            } catch (Exception e) {
                                log.warn("SSE parse warn: {}", e.getMessage());
                            }
                        }
                    }
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("Stream error: {}", e.getMessage());
                emitter.error(e);
            }
        });
    }

    /**
     * 智谱API非流式对话（用于意图识别等一次性调用）
     */
    public String chatSync(List<Map<String, String>> messages) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);

        ArrayNode messagesArray = requestBody.putArray("messages");
        for (Map<String, String> msg : messages) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.get("role"));
            msgNode.put("content", msg.get("content"));
        }

        requestBody.put("stream", false);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 1024);

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) return "";

            JsonNode node = objectMapper.readTree(response);
            return node.path("choices").get(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.error("Chat sync error: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 生成文本向量嵌入
     */
    public List<Double> createEmbedding(String text) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", embeddingModel);
        requestBody.put("input", text);

        try {
            String response = webClient.post()
                    .uri("/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) return Collections.emptyList();

            JsonNode node = objectMapper.readTree(response);
            JsonNode embeddingData = node.path("data").get(0).path("embedding");
            List<Double> embedding = new ArrayList<>();
            for (JsonNode val : embeddingData) {
                embedding.add(val.asDouble());
            }
            return embedding;
        } catch (Exception e) {
            log.error("Embedding generation error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
