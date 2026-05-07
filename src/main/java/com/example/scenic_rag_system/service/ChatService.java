package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.dto.ChatResponse;
import com.example.scenic_rag_system.dto.IntentResult;
import com.example.scenic_rag_system.dto.VectorDocument;
import com.example.scenic_rag_system.entity.FixedQA;
import com.example.scenic_rag_system.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChromaVectorService chromaVectorService;
    private final IntentRecognitionService intentRecognitionService;
    private final ObjectMapper objectMapper;
    private final SessionService sessionService;
    private final MessageService messageService;

    private final AttractionRepository attractionRepo;
    private final FoodShopRepository foodShopRepo;
    private final HotelRepository hotelRepo;
    private final LocalProductRepository localProductRepo;
    private final TicketRepository ticketRepo;

    @Value("${zhipu.api-key}") private String apiKey;
    @Value("${zhipu.model}") private String model;
    @Value("${zhipu.base-url}") private String baseUrl;
    @Value("${app.fixed-qa-threshold}") private double fixedQaThreshold;
    @Value("${app.rag-similarity-threshold}") private double ragSimilarityThreshold;

    public void processMessageStream(String userMessage, Long sessionId, Long userId,
                                      SseEmitter emitter, ObjectMapper om) {
        try {
            if (sessionId == null) {
                sessionId = sessionService.createByUserId(userId).getId();
            }
            messageService.addMessage(sessionId, "user", userMessage, null);

            List<Map<String, Object>> history = messageService.getMessages(sessionId);
            List<Map<String, String>> chatHistory = new ArrayList<>();
            int start = Math.max(0, history.size() - 11);
            for (int i = start; i < history.size() - 1; i++) {
                Map<String, Object> m = history.get(i);
                chatHistory.add(Map.of("role", (String) m.get("role"), "content", (String) m.get("content")));
            }

            FixedQA matched = searchFixedQA(userMessage);
            if (matched != null) {
                messageService.addMessage(sessionId, "assistant", matched.getAnswer(), null);
                sendJson(emitter, om, ChatResponse.builder().content(matched.getAnswer()).intentType("固定回答").build());
                sendDone(emitter);
                return;
            }

            IntentResult intent = intentRecognitionService.recognize(userMessage, chatHistory);
            log.info("Intent: {} params: {}", intent.getIntentType(), intent.getParams());

            if (history.size() <= 2) {
                String title = userMessage.length() > 30 ? userMessage.substring(0, 28) + "..." : userMessage;
                sessionService.updateTitle(sessionId, title);
            }

            ChatResponse.CardData[] cardHolder = new ChatResponse.CardData[1];
            StringBuilder fullContent = new StringBuilder();

            switch (intent.getIntentType()) {
                case "景点讲解" -> ragGen("attraction", intent, userMessage, chatHistory, emitter, om, cardHolder, fullContent);
                case "美食导购" -> ragGen("food", intent, userMessage, chatHistory, emitter, om, cardHolder, fullContent);
                case "酒店导购" -> ragGen("hotel", intent, userMessage, chatHistory, emitter, om, cardHolder, fullContent);
                case "特产文创导购" -> ragGen("product", intent, userMessage, chatHistory, emitter, om, cardHolder, fullContent);
                case "天气查询" -> streamLlm(buildWeatherPrompt(intent), userMessage, chatHistory, emitter, om, "天气查询", cardHolder, fullContent);
                case "门票购买" -> ragGen("ticket", intent, userMessage, chatHistory, emitter, om, cardHolder, fullContent);
                case "客服问答" -> streamLlm("你是一位洛阳景区客服助手。请友好地回答游客关于洛阳旅游的问题。", userMessage, chatHistory, emitter, om, "客服问答", cardHolder, fullContent);
                case "拒绝回答" -> {
                    String r = "抱歉，我无法回答这个问题。请咨询与洛阳旅游相关的信息。";
                    messageService.addMessage(sessionId, "assistant", r, null);
                    sendJson(emitter, om, ChatResponse.builder().content(r).intentType("拒绝回答").build());
                    sendDone(emitter);
                    return;
                }
                default -> streamLlm("您好！我是洛阳景区智能助手，可以为您提供景点介绍、美食推荐等服务。", userMessage, chatHistory, emitter, om, "客服问答", cardHolder, fullContent);
            }

            if (fullContent.length() > 0) {
                messageService.addMessage(sessionId, "assistant", fullContent.toString(), cardHolder[0]);
            }
        } catch (Exception e) {
            log.error("Chat error: {}", e.getMessage());
            sendJsonSafe(emitter, om, ChatResponse.builder().content("抱歉，处理请求时出错。").intentType("error").build());
            sendDone(emitter);
        }
    }

    private void ragGen(String collection, IntentResult intent, String userMessage,
                         List<Map<String, String>> history, SseEmitter emitter,
                         ObjectMapper om, ChatResponse.CardData[] cardHolder, StringBuilder fullContent) {
        String q = buildQueryText(
                intent.getParams().getOrDefault("景点", intent.getParams().getOrDefault("出发地", "")),
                intent.getParams().getOrDefault("标签", ""), userMessage);
        var results = chromaVectorService.search(collection, q, 5);
        var filtered = results.stream().filter(d -> d.getScore() >= ragSimilarityThreshold).toList();

        String template = switch (collection) {
            case "attraction" -> "你是一位洛阳景区讲解员。根据以下景点信息，用优美的语言介绍景点，突出历史文化特色。\n\n{rag_context}\n\n用自然的口吻回答。";
            case "food" -> "你是一位洛阳美食向导。根据以下美食信息推荐餐厅，突出特色菜品。\n\n{rag_context}";
            case "hotel" -> "你是一位洛阳酒店顾问。根据以下酒店信息推荐住宿，突出设施和位置。\n\n{rag_context}";
            case "product" -> "你是一位洛阳特产推荐官。根据以下特产信息推荐产品，突出文化内涵。\n\n{rag_context}";
            case "ticket" -> "你是一位洛阳景区门票顾问。根据以下门票信息介绍价格和种类，提供购票建议。\n\n{rag_context}";
            default -> "根据以下信息回答：\n{rag_context}";
        };

        String prompt;
        if (!filtered.isEmpty()) {
            String ctx = filtered.stream()
                    .map(d -> "- " + (d.getMetadata() != null ? d.getMetadata().getOrDefault("name", "") : "") + ": " + d.getText())
                    .collect(Collectors.joining("\n"));
            prompt = template.replace("{rag_context}", ctx);
            cardHolder[0] = buildCard(filtered.get(0), collection);
        } else {
            prompt = "用户询问了关于洛阳" + typeName(collection) + "的问题，请根据你的知识回答。";
        }
        streamLlm(prompt, userMessage, history, emitter, om, intent.getIntentType(), cardHolder, fullContent);
    }

    private void streamLlm(String systemPrompt, String userMessage, List<Map<String, String>> history,
                            SseEmitter emitter, ObjectMapper om, String intentType,
                            ChatResponse.CardData[] cardHolder, StringBuilder fullContent) {
        try {
            List<Map<String, String>> msgs = new ArrayList<>();
            msgs.add(Map.of("role", "system", "content", systemPrompt));
            if (history != null) {
                int s = Math.max(0, history.size() - 6);
                for (int i = s; i < history.size(); i++) msgs.add(history.get(i));
            }
            msgs.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> body = new HashMap<>();
            body.put("model", model); body.put("stream", true);
            body.put("temperature", 0.7); body.put("max_tokens", 2048);
            body.put("messages", msgs);
            String reqBody = objectMapper.writeValueAsString(body);

            HttpURLConnection conn = (HttpURLConnection) URI.create(baseUrl + "/chat/completions").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);
            try (OutputStream os = conn.getOutputStream()) { os.write(reqBody.getBytes(StandardCharsets.UTF_8)); }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;
                        String c = objectMapper.readTree(data).path("choices").get(0).path("delta").path("content").asText("");
                        if (!c.isEmpty()) {
                            fullContent.append(c);
                            sendJson(emitter, om, ChatResponse.builder().content(c).intentType(intentType).card(cardHolder[0]).build());
                        }
                    }
                }
            }
            if (cardHolder[0] != null) {
                sendJson(emitter, om, ChatResponse.builder().content("").intentType(intentType).card(cardHolder[0]).build());
            }
            sendDone(emitter);
        } catch (Exception e) {
            log.error("Zhipu error: {}", e.getMessage());
            sendJsonSafe(emitter, om, ChatResponse.builder().content("抱歉，AI服务暂时不可用。").intentType(intentType).build());
            sendDone(emitter);
        }
    }

    private String buildWeatherPrompt(IntentResult intent) {
        return "当前天气信息：\n地点：" + intent.getParams().getOrDefault("地点", "洛阳")
                + "\n时间：" + intent.getParams().getOrDefault("时间", "今天")
                + "\n天气：晴\n气温：18-28℃\n风力：东南风2-3级\n空气质量：优"
                + "\n\n请根据以上信息回答用户关于天气的询问，并给出出行建议。";
    }

    private FixedQA searchFixedQA(String query) {
        try {
            var results = chromaVectorService.search("fixed_qa", query, 1);
            if (results != null && !results.isEmpty() && results.get(0).getScore() >= fixedQaThreshold
                    && results.get(0).getMetadata() != null && results.get(0).getMetadata().containsKey("answer")) {
                FixedQA qa = new FixedQA();
                qa.setAnswer((String) results.get(0).getMetadata().get("answer"));
                return qa;
            }
        } catch (Exception e) { log.warn("Fixed QA error: {}", e.getMessage()); }
        return null;
    }

    private void sendJson(SseEmitter emitter, ObjectMapper om, Object data) {
        try {
            emitter.send(SseEmitter.event().name("message").data(om.writeValueAsString(data), org.springframework.http.MediaType.APPLICATION_JSON));
        } catch (Exception e) { log.warn("SSE error: {}", e.getMessage()); }
    }

    private void sendJsonSafe(SseEmitter emitter, ObjectMapper om, Object data) {
        try { sendJson(emitter, om, data); } catch (Exception ignored) {}
    }

    private void sendDone(SseEmitter emitter) {
        try { emitter.send(SseEmitter.event().name("done").data("[DONE]")); emitter.complete(); } catch (Exception ignored) {}
    }

    private String buildQueryText(String a, String b, String fallback) {
        String t = (a + " " + b).trim(); return t.isEmpty() ? fallback : t;
    }

    private ChatResponse.CardData buildCard(VectorDocument doc, String type) {
        if (doc.getMetadata() == null || doc.getMetadata().get("id") == null) return null;
        Long id = Long.parseLong((String) doc.getMetadata().get("id"));
        Map<String, Object> d = new HashMap<>();
        switch (type) {
            case "attraction" -> attractionRepo.findById(id).ifPresent(a -> { d.put("name", a.getName()); d.put("image", a.getImage()); d.put("description", a.getDescription()); d.put("address", a.getAddress()); d.put("tags", splitTags(a.getTags())); });
            case "food" -> foodShopRepo.findById(id).ifPresent(f -> { d.put("name", f.getName()); d.put("image", f.getImage()); d.put("address", f.getAddress()); d.put("tags", splitTags(f.getTags())); });
            case "hotel" -> hotelRepo.findById(id).ifPresent(h -> { d.put("name", h.getName()); d.put("image", h.getImage()); d.put("address", h.getAddress()); d.put("level", h.getLevel()); d.put("price", h.getPrice()); });
            case "ticket" -> ticketRepo.findById(id).ifPresent(t -> { d.put("name", t.getName()); d.put("image", t.getImage()); d.put("price", t.getPrice()); });
            case "product" -> localProductRepo.findById(id).ifPresent(p -> { d.put("name", p.getName()); d.put("image", p.getImage()); d.put("category", p.getCategory()); d.put("price", p.getPrice()); });
        }
        return d.isEmpty() ? null : ChatResponse.CardData.builder().type(type).data(d).build();
    }

    private List<String> splitTags(String tags) {
        return tags == null || tags.isBlank() ? List.of() : Arrays.stream(tags.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private String typeName(String t) {
        return switch (t) { case "attraction" -> "景点"; case "food" -> "美食"; case "hotel" -> "酒店"; case "product" -> "特产"; case "ticket" -> "门票"; default -> t; };
    }
}
