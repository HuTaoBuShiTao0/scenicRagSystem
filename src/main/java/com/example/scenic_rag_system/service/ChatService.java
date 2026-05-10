package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.controller.PromptController;
import com.example.scenic_rag_system.dto.ChatResponse;
import com.example.scenic_rag_system.dto.IntentResult;
import com.example.scenic_rag_system.dto.VectorDocument;
import com.example.scenic_rag_system.dto.WeatherResult;
import com.example.scenic_rag_system.entity.FixedQA;
import com.example.scenic_rag_system.entity.Prompt;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    private final PromptRepository promptRepository;
    private final CustomerServiceRepository customerServiceRepo;
    private final QWeatherService qWeatherService;

    @Value("${zhipu.api-key}") private String apiKey;
    @Value("${zhipu.model}") private String model;
    @Value("${zhipu.base-url}") private String baseUrl;
    @Value("${app.fixed-qa-threshold}") private double fixedQaThreshold;
    @Value("${app.rag-similarity-threshold}") private double ragSimilarityThreshold;

    public void processMessageStream(String userMessage, Long sessionId, Long userId,
                                      SseEmitter emitter, ObjectMapper om) {
        try {
            boolean isNewSession = false;
            if (sessionId == null) {
                sessionId = sessionService.createByUserId(userId).getId();
                isNewSession = true;
            }
            // 将会话 ID 返回给前端（新建或已有都返回，确保前端同步）
            sendJsonSafe(emitter, om, ChatResponse.builder().sessionId(sessionId).build());
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

            List<ChatResponse.CardData> cardList = new ArrayList<>();
            StringBuilder fullContent = new StringBuilder();

            switch (intent.getIntentType()) {
                case "景点讲解" -> ragGen("attraction", intent, userMessage, chatHistory, emitter, om, cardList, fullContent);
                case "美食导购" -> ragGen("food", intent, userMessage, chatHistory, emitter, om, cardList, fullContent);
                case "酒店导购" -> ragGen("hotel", intent, userMessage, chatHistory, emitter, om, cardList, fullContent);
                case "特产文创导购" -> ragGen("product", intent, userMessage, chatHistory, emitter, om, cardList, fullContent);
                case "天气查询" -> {
                    String location = intent.getParams().getOrDefault("地点", "洛阳");
                    if (location == null || location.isBlank()) location = "洛阳";
                    String timeParam = intent.getParams().getOrDefault("时间", "今天");
                    // 多天查询用 7d，单天用 3d
                    boolean isMultiDay = isMultiDayQuery(timeParam);
                    String days = isMultiDay ? "7d" : "3d";
                    List<WeatherResult> forecasts = null;
                    try {
                        forecasts = qWeatherService.getWeatherForecast(location, days);
                    } catch (Exception e) {
                        log.warn("Weather fetch failed for card: {}", e.getMessage());
                    }
                    if (forecasts != null && !forecasts.isEmpty()) {
                        if (isMultiDay) {
                            // 多天：每天一张卡片
                            for (WeatherResult w : forecasts) {
                                cardList.add(buildWeatherCard(w, location));
                            }
                            log.info("Weather cards added for all {} days (timeParam={})", forecasts.size(), timeParam);
                        } else {
                            // 单天：根据时间参数选
                            int dayIndex = resolveDayIndex(timeParam, forecasts);
                            if (dayIndex >= 0 && dayIndex < forecasts.size()) {
                                cardList.add(buildWeatherCard(forecasts.get(dayIndex), location));
                                log.info("Weather card added for dayIndex={} (timeParam={})", dayIndex, timeParam);
                            } else {
                                cardList.add(buildWeatherCard(forecasts.get(0), location));
                                log.info("Weather card added fallback index 0 (timeParam={})", timeParam);
                            }
                        }
                    } else {
                        log.warn("No weather forecast data available for card, timeParam={}", timeParam);
                    }
                    String weatherPrompt = buildWeatherPromptFromTemplate(intent, userMessage, chatHistory, forecasts);
                    streamLlm(weatherPrompt, userMessage, chatHistory, emitter, om, "天气查询", cardList, fullContent);
                }
                case "门票购买" -> ragGen("ticket", intent, userMessage, chatHistory, emitter, om, cardList, fullContent);
                case "客服问答" -> {
                    String csPrompt = buildCustomerServicePrompt(userMessage);
                    streamLlm(csPrompt, userMessage, chatHistory, emitter, om, "客服问答", cardList, fullContent);
                }
                case "拒绝回答" -> {
                    String r = "抱歉，我无法回答这个问题。请咨询与洛阳旅游相关的信息。";
                    messageService.addMessage(sessionId, "assistant", r, null);
                    sendJson(emitter, om, ChatResponse.builder().content(r).intentType("拒绝回答").build());
                    sendDone(emitter);
                    return;
                }
                default -> streamLlm("您好！我是洛阳景区智能助手，可以为您提供景点介绍、美食推荐等服务。", userMessage, chatHistory, emitter, om, "客服问答", cardList, fullContent);
            }

            if (fullContent.length() > 0) {
                messageService.addMessage(sessionId, "assistant", fullContent.toString(),
                        cardList.isEmpty() ? null : cardList);
            }
        } catch (Exception e) {
            log.error("Chat error: {}", e.getMessage());
            sendJsonSafe(emitter, om, ChatResponse.builder().content("抱歉，处理请求时出错。").intentType("error").build());
            sendDone(emitter);
        }
    }

    /**
     * 子意图回复提示词的 DB 类型前缀
     */
    private static final String REPLY_PROMPT_PREFIX = "REPLY_";

    /**
     * 内置默认回复提示词（DB中无配置时使用）
     */
    private static final Map<String, String> DEFAULT_REPLY_PROMPTS = new HashMap<>();
    static {
        DEFAULT_REPLY_PROMPTS.put("attraction", "你是一位洛阳景区讲解员。根据以下景点信息，用优美的语言介绍景点，突出历史文化特色。\n\n{rag_context}\n\n用自然的口吻回答。");
        DEFAULT_REPLY_PROMPTS.put("food", "你是一位洛阳美食向导。根据以下美食信息推荐餐厅，突出特色菜品。\n\n{rag_context}");
        DEFAULT_REPLY_PROMPTS.put("hotel", "你是一位洛阳酒店顾问。根据以下酒店信息推荐住宿，突出设施和位置。\n\n{rag_context}");
        DEFAULT_REPLY_PROMPTS.put("product", "你是一位洛阳特产推荐官。根据以下特产信息推荐产品，突出文化内涵。\n\n{rag_context}");
        DEFAULT_REPLY_PROMPTS.put("ticket", "你是一位洛阳景区门票顾问。根据以下门票信息介绍价格和种类，提供购票建议。\n\n{rag_context}");
    }

    /**
     * 获取指定集合类型的回复提示词
     * 优先级：缓存 > 数据库 > 内置默认值
     */
    private String getReplyPrompt(String collection) {
        String promptType = REPLY_PROMPT_PREFIX + collection.toUpperCase();
        // 从缓存读取
        String cached = PromptController.promptCache.get(promptType);
        if (cached != null) {
            return cached;
        }
        // 从数据库加载
        try {
            Optional<Prompt> promptOpt = promptRepository.findByType(promptType);
            if (promptOpt.isPresent() && promptOpt.get().getEnabled()) {
                String content = promptOpt.get().getContent();
                PromptController.promptCache.put(promptType, content);
                return content;
            }
        } catch (Exception e) {
            log.warn("Failed to load reply prompt for '{}' from DB, using default", collection);
        }
        // 使用内置默认值
        return DEFAULT_REPLY_PROMPTS.getOrDefault(collection, "根据以下信息回答：\n{rag_context}");
    }

    /**
     * 获取无搜索结果时的备用提示词
     */
    private String getNoResultPrompt(String collection) {
        String promptType = REPLY_PROMPT_PREFIX + collection.toUpperCase() + "_NO_RESULT";
        String cached = PromptController.promptCache.get(promptType);
        if (cached != null) return cached;
        try {
            Optional<Prompt> promptOpt = promptRepository.findByType(promptType);
            if (promptOpt.isPresent() && promptOpt.get().getEnabled()) {
                String content = promptOpt.get().getContent();
                PromptController.promptCache.put(promptType, content);
                return content;
            }
        } catch (Exception e) {
            log.warn("Failed to load no-result prompt for '{}'", collection);
        }
        return "用户询问了关于洛阳" + typeName(collection) + "的问题，请根据你的知识回答。";
    }

    /**
     * 将对话历史格式化为文本，用于替换模板中的 {history} 占位符
     */
    private String formatHistory(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) return "";
        return history.stream()
                .map(m -> (m.get("role") + ": " + m.get("content")))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 构建客服问答提示词，从 DB 读取模板并替换占位符
     */
    private String buildCustomerServicePrompt(String userMessage) {
        String promptType = "REPLY_CUSTOMER_SERVICE";
        String cached = PromptController.promptCache.get(promptType);
        String template = null;
        if (cached != null) {
            template = cached;
        } else {
            try {
                Optional<Prompt> promptOpt = promptRepository.findByType(promptType);
                if (promptOpt.isPresent() && promptOpt.get().getEnabled()) {
                    template = promptOpt.get().getContent();
                    PromptController.promptCache.put(promptType, template);
                }
            } catch (Exception e) {
                log.warn("Failed to load customer service prompt from DB");
            }
        }
        if (template == null) {
            template = "你是一位洛阳景区客服助手。请友好地回答游客关于洛阳旅游的问题。";
        }

        // 从客服知识库中检索匹配的内容
        String ragContext = searchCustomerService(userMessage);

        LocalDate now = LocalDate.now();
        String currentDate = now.format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        String dayOfWeek = getChineseDayOfWeek(now.getDayOfWeek().getValue());

        return template
                .replace("{rag_context}", ragContext)
                .replace("{user_message}", userMessage)
                .replace("{service_entity}", "洛阳景区")
                .replace("{agent_name}", "洛阳景区智能客服")
                .replace("{current_date}", currentDate)
                .replace("{day_of_week}", dayOfWeek);
    }

    /**
     * 搜索客服知识库，返回匹配的上下文文本
     */
    private String searchCustomerService(String query) {
        if (query == null || query.isBlank()) return "";
        try {
            // 提取用户查询中的关键词（中文逐字 + 英文单词）
            java.util.Set<String> queryTokens = new java.util.HashSet<>();
            StringBuilder word = new StringBuilder();
            for (int i = 0; i < query.length(); i++) {
                char c = query.charAt(i);
                if (Character.isIdeographic(c)) {
                    queryTokens.add(String.valueOf(c));
                } else if (Character.isLetterOrDigit(c)) {
                    word.append(c);
                } else {
                    if (word.length() > 0) {
                        queryTokens.add(word.toString().toLowerCase());
                        word = new StringBuilder();
                    }
                }
            }
            if (word.length() > 0) queryTokens.add(word.toString().toLowerCase());

            if (queryTokens.isEmpty()) return "";

            // 遍历客服知识库，按关键词匹配
            List<com.example.scenic_rag_system.entity.CustomerService> allEntries = customerServiceRepo.findAll();
            StringBuilder matched = new StringBuilder();

            for (var entry : allEntries) {
                if (entry.getKeywords() == null || entry.getKeywords().isBlank()) continue;
                String[] keywords = entry.getKeywords().split(",");
                int matchCount = 0;
                for (String kw : keywords) {
                    String trimmedKw = kw.trim();
                    if (trimmedKw.isEmpty()) continue;
                    // 检查用户查询中是否包含该关键词
                    if (query.contains(trimmedKw)) {
                        matchCount++;
                    } else {
                        // 也检查用户查询的字是否匹配关键词中的字
                        for (String qt : queryTokens) {
                            if (trimmedKw.contains(qt) && qt.length() >= 2) {
                                matchCount++;
                                break;
                            }
                        }
                    }
                }
                // 至少匹配一个关键词才认为相关
                if (matchCount > 0) {
                    matched.append("- [").append(entry.getKeywords()).append("] ")
                            .append(entry.getResponse()).append("\n\n");
                }
            }

            String result = matched.toString().trim();
            if (!result.isEmpty()) {
                log.info("Customer service KB matched {} entries for query '{}'", 
                    result.split("\n\n").length, query);
            }
            return result;
        } catch (Exception e) {
            log.warn("Customer service KB search error: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 获取中文星期几
     */
    private String getChineseDayOfWeek(int dayValue) {
        return switch (dayValue) {
            case 1 -> "星期一";
            case 2 -> "星期二";
            case 3 -> "星期三";
            case 4 -> "星期四";
            case 5 -> "星期五";
            case 6 -> "星期六";
            case 7 -> "星期日";
            default -> "";
        };
    }

    private void ragGen(String collection, IntentResult intent, String userMessage,
                         List<Map<String, String>> history, SseEmitter emitter,
                         ObjectMapper om, List<ChatResponse.CardData> cardList, StringBuilder fullContent) {
        String q = buildQueryText(
                intent.getParams().getOrDefault("景点", intent.getParams().getOrDefault("出发地", "")),
                intent.getParams().getOrDefault("标签", ""), userMessage);
        var results = chromaVectorService.search(collection, q, 5);
        var filtered = results.stream().filter(d -> d.getScore() >= ragSimilarityThreshold).toList();

        String prompt;
        if (!filtered.isEmpty()) {
            String ctx = filtered.stream()
                    .map(d -> "- " + (d.getMetadata() != null ? d.getMetadata().getOrDefault("name", "") : "") + ": " + d.getText())
                    .collect(Collectors.joining("\n"));
            String template = getReplyPrompt(collection);
            String formattedHistory = formatHistory(history);
            prompt = template.replace("{rag_context}", ctx)
                    .replace("{user_message}", userMessage)
                    .replace("{history}", formattedHistory);
            // 构建召回结果的卡片，按相关性排序，过滤归属不匹配的景点
            String scenicFilter = intent.getParams().getOrDefault("景点", "");
            // 也尝试从"出发地"参数获取过滤条件（如美食导购的附近推荐）
            String locationFilter = intent.getParams().getOrDefault("出发地", "");
            String filter = !scenicFilter.isEmpty() ? scenicFilter : locationFilter;

            for (var doc : filtered) {
                var card = buildCard(doc, collection);
                if (card == null) continue;
                // 归属过滤：如果用户指定了具体景区/地点，只保留归属匹配的卡片
                if (!filter.isEmpty()) {
                    // 如果文档名称本身就匹配查询（如直接搜"奉先寺"），不过滤
                    String docName = doc.getMetadata() != null
                            ? (String) doc.getMetadata().get("name") : "";
                    if (docName != null && !docName.isEmpty()
                            && (docName.contains(filter) || filter.contains(docName))) {
                        // 文档名称匹配查询，保留
                    } else {
                        String docLocation = getEntityLocation(collection, doc);
                        if (docLocation != null && !docLocation.contains(filter)
                                && !filter.contains(docLocation)) {
                            log.debug("Card filtered (location mismatch): {} vs {}", docLocation, filter);
                            continue;
                        }
                    }
                }
                cardList.add(card);
            }
        } else {
            prompt = getNoResultPrompt(collection);
        }
        streamLlm(prompt, userMessage, history, emitter, om, intent.getIntentType(), cardList, fullContent);
    }

    private void streamLlm(String systemPrompt, String userMessage, List<Map<String, String>> history,
                            SseEmitter emitter, ObjectMapper om, String intentType,
                            List<ChatResponse.CardData> cardList, StringBuilder fullContent) {
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
                            // 流式传输仅发文本内容，卡片在最后统一发送
                            sendJson(emitter, om, ChatResponse.builder().content(c).intentType(intentType).build());
                        }
                    }
                }
            }
            // 流式结束后统一发送所有卡片（按相关性排序）
            if (!cardList.isEmpty()) {
                sendJson(emitter, om, ChatResponse.builder()
                        .content("").intentType(intentType).cards(cardList).build());
            }
            sendDone(emitter);
        } catch (Exception e) {
            log.error("Zhipu error: {}", e.getMessage());
            sendJsonSafe(emitter, om, ChatResponse.builder().content("抱歉，AI服务暂时不可用。").intentType(intentType).build());
            sendDone(emitter);
        }
    }

    /**
     * 构建天气查询提示词，从 DB 读取模板并替换占位符
     */
    private String buildWeatherPromptFromTemplate(IntentResult intent, String userMessage,
                                                   List<Map<String, String>> history) {
        return buildWeatherPromptFromTemplate(intent, userMessage, history, null);
    }

    private String buildWeatherPromptFromTemplate(IntentResult intent, String userMessage,
                                                   List<Map<String, String>> history,
                                                   List<WeatherResult> preFetchedForecasts) {
        String location = intent.getParams().getOrDefault("地点", "洛阳");
        if (location == null || location.isBlank()) location = "洛阳";
        String timeParam = intent.getParams().getOrDefault("时间", "今天");

        // 获取天气数据（格式化文本）
        String weatherData = fetchWeatherData(location, preFetchedForecasts, timeParam);

        // 从 DB 加载模板
        String promptType = "REPLY_WEATHER";
        String cached = PromptController.promptCache.get(promptType);
        String template = null;
        if (cached != null) {
            template = cached;
        } else {
            try {
                Optional<Prompt> promptOpt = promptRepository.findByType(promptType);
                if (promptOpt.isPresent() && promptOpt.get().getEnabled()) {
                    template = promptOpt.get().getContent();
                    PromptController.promptCache.put(promptType, template);
                }
            } catch (Exception e) {
                log.warn("Failed to load weather prompt from DB");
            }
        }
        if (template == null) {
            // 内置默认模板
            template = "##角色\n你是一个天气预报助手\n\n"
                    + "##要求\n1、回复要求简洁明了,回答内容字符不超过50个字\n"
                    + "2、参考天气预报回答\n3、禁止使用markdown格式输出\n\n"
                    + "##当前时刻\n{current_date},{day_of_week}\n\n"
                    + "##用户问题\n{user_message}\n\n"
                    + "##天气预报信息：\n{weather_data}\n\n"
                    + "##请回答：";
        }

        LocalDate now = LocalDate.now();
        String currentDate = now.format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        String dayOfWeek = getChineseDayOfWeek(now.getDayOfWeek().getValue());

        return template
                .replace("{weather_data}", weatherData)
                .replace("{current_date}", currentDate)
                .replace("{day_of_week}", dayOfWeek)
                .replace("{user_message}", userMessage);
    }

    /**
     * 调和风天气 API 获取天气数据文本
     */
    private String fetchWeatherData(String location) {
        return fetchWeatherData(location, null, "今天");
    }

    private String fetchWeatherData(String location, List<WeatherResult> preFetched) {
        return fetchWeatherData(location, preFetched, "今天");
    }

    private String fetchWeatherData(String location, List<WeatherResult> preFetched, String timeParam) {
        try {
            boolean multiDay = isMultiDayQuery(timeParam);
            List<WeatherResult> forecasts = preFetched != null ? preFetched
                    : qWeatherService.getWeatherForecast(location, multiDay ? "7d" : "3d");
            if (forecasts != null && !forecasts.isEmpty()) {
                if (multiDay) {
                    // 多天查询：返回全部数据
                    StringBuilder sb = new StringBuilder();
                    String[] labels = {"今天", "明天", "后天"};
                    for (int i = 0; i < forecasts.size(); i++) {
                        WeatherResult d = forecasts.get(i);
                        String label = i < labels.length ? labels[i] : d.getFxDate();
                        sb.append("--- ").append(label).append(" ---\n");
                        sb.append("天气：").append(d.getTextDay()).append("\n");
                        sb.append("气温：").append(d.getTempMin()).append("~").append(d.getTempMax()).append("℃\n");
                        sb.append("风力：").append(d.getWindDirDay()).append(d.getWindScaleDay()).append("级\n");
                        sb.append("湿度：").append(d.getHumidity()).append("%\n");
                        sb.append("降水量：").append(d.getPrecip()).append("mm\n\n");
                    }
                    return sb.toString();
                } else {
                    int idx = resolveDayIndex(timeParam, forecasts);
                    WeatherResult day = forecasts.get(idx);
                    String dayLabel = switch (idx) {
                        case 1 -> "明天";
                        case 2 -> "后天";
                        default -> "今天";
                    };
                    return "日期：" + dayLabel + "（" + day.getFxDate() + "）"
                            + "\n地点：" + location
                            + "\n天气：" + day.getTextDay()
                            + "\n气温：" + day.getTempMin() + "~" + day.getTempMax() + "℃"
                            + "\n风力：" + day.getWindDirDay() + day.getWindScaleDay() + "级"
                            + "\n湿度：" + day.getHumidity() + "%"
                            + "\n降水量：" + day.getPrecip() + "mm";
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch real weather data: {}", e.getMessage());
        }
        return "地点：" + location + "\n天气：晴\n气温：18-28℃\n风力：东南风2-3级\n空气质量：优";
    }

    /**
     * 解析时间参数对应的天气预报索引
     * 支持：今天/明日→0，明天→1，后天→2，也尝试按 fxDate 日期匹配
     */
    private int resolveDayIndex(String timeParam, List<WeatherResult> forecasts) {
        if (timeParam == null || forecasts == null || forecasts.isEmpty()) return 0;
        log.info("Resolving weather day index for timeParam='{}'", timeParam);

        // 1. 按关键词匹配
        if (timeParam.contains("明天") || timeParam.contains("明日")) {
            return Math.min(1, forecasts.size() - 1);
        }
        if (timeParam.contains("后天")) {
            return Math.min(2, forecasts.size() - 1);
        }

        // 2. 如果是具体日期格式（如 2026-05-11），尝试在预报中匹配
        String dateStr = timeParam.trim();
        if (dateStr.contains("-") || dateStr.matches("\\d{8}")) {
            // 截取日期部分
            if (dateStr.length() >= 10) dateStr = dateStr.substring(0, 10);
            for (int i = 0; i < forecasts.size(); i++) {
                if (forecasts.get(i).getFxDate() != null && forecasts.get(i).getFxDate().contains(dateStr)) {
                    return i;
                }
            }
        }

        // 3. 默认今天
        return 0;
    }

    /**
     * 判断是否为多天查询（未来一周、未来几天等）
     */
    private boolean isMultiDayQuery(String timeParam) {
        if (timeParam == null) return false;
        return timeParam.contains("一周") || timeParam.contains("未来")
                || timeParam.contains("最近") || timeParam.contains("这几天")
                || timeParam.contains("多天") || timeParam.contains("这周")
                || timeParam.contains("本周") || timeParam.contains("近期");
    }

    /**
     * 构建天气卡片
     */
    private ChatResponse.CardData buildWeatherCard(WeatherResult weather, String location) {
        Map<String, Object> d = new HashMap<>();
        d.put("location", location);
        d.put("tempMin", weather.getTempMin());
        d.put("tempMax", weather.getTempMax());
        d.put("textDay", weather.getTextDay());
        d.put("windDirDay", weather.getWindDirDay());
        d.put("windScaleDay", weather.getWindScaleDay());
        d.put("humidity", weather.getHumidity());
        d.put("precip", weather.getPrecip());
        d.put("uvIndex", weather.getUvIndex());
        return ChatResponse.CardData.builder().type("weather").data(d).build();
    }

    /**
     * 搜索固定问答。使用向量相似度 + 字符重叠率双重校验。
     * 避免像"推荐必去景点"匹配到"老君山怎么玩"这类误匹配。
     */
    private FixedQA searchFixedQA(String query) {
        try {
            var results = chromaVectorService.search("fixed_qa", query, 3);
            if (results == null || results.isEmpty()) return null;

            for (var doc : results) {
                if (doc.getScore() < fixedQaThreshold) continue;
                if (doc.getMetadata() == null || !doc.getMetadata().containsKey("answer")) continue;

                String matchedQuestion = doc.getText();
                if (matchedQuestion != null && !matchedQuestion.isBlank()) {
                    // 字符重叠率校验：匹配问题与用户查询必须共享至少 40% 的字符
                    double overlap = characterOverlap(query, matchedQuestion);
                    if (overlap < 0.40) {
                        log.info("Fixed QA skipped (overlap {} < 0.40): query='{}' matched='{}'",
                                String.format("%.2f", overlap), query, matchedQuestion);
                        continue;
                    }
                }

                FixedQA qa = new FixedQA();
                qa.setAnswer((String) doc.getMetadata().get("answer"));
                return qa;
            }
        } catch (Exception e) { log.warn("Fixed QA error: {}", e.getMessage()); }
        return null;
    }

    /**
     * 计算两个字符串的字符重叠率（基于字符集合）
     * 例如 "推荐几个洛阳必去的景点" vs "洛阳老君山怎么玩" → 18%
     */
    private double characterOverlap(String a, String b) {
        java.util.Set<Character> setA = new java.util.HashSet<>();
        for (char c : a.toCharArray()) setA.add(c);
        java.util.Set<Character> setB = new java.util.HashSet<>();
        for (char c : b.toCharArray()) setB.add(c);

        int intersection = 0;
        for (char c : setA) {
            if (setB.contains(c)) intersection++;
        }
        int union = setA.size() + setB.size() - intersection;
        return union == 0 ? 0 : (double) intersection / union;
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

    /**
     * 构建富媒体卡片。type 是集合名，返回的卡片 type 映射为前端可识别的 cardType。
     * 每个卡片都带全字段，前端根据 cardType 选择性展示。
     */
    private ChatResponse.CardData buildCard(VectorDocument doc, String collection) {
        if (doc.getMetadata() == null || doc.getMetadata().get("id") == null) return null;
        Long id = Long.parseLong((String) doc.getMetadata().get("id"));
        Map<String, Object> d = new HashMap<>();

        // 后端集合名 → 前端卡片类型映射
        String cardType = switch (collection) {
            case "attraction" -> "attraction";
            case "food" -> "food";
            case "hotel" -> "hotel";
            case "product" -> "product";
            case "ticket" -> "ticket";
            default -> collection;
        };

        switch (collection) {
            case "attraction" -> attractionRepo.findById(id).ifPresent(a -> {
                d.put("name", a.getName());
                d.put("image", a.getImage());
                d.put("tags", splitTags(a.getTags()));
                d.put("relatedScenic", a.getRelatedScenic());
                d.put("briefIntro", a.getBriefIntro());
                d.put("description", a.getDescription());
                d.put("address", a.getAddress());
            });
            case "food" -> foodShopRepo.findById(id).ifPresent(f -> {
                d.put("name", f.getName());
                d.put("image", f.getImage());
                d.put("tags", splitTags(f.getTags()));
                d.put("relatedScenic", f.getRelatedScenic());
                d.put("address", f.getAddress());
                d.put("shopName", f.getName());
            });
            case "hotel" -> hotelRepo.findById(id).ifPresent(h -> {
                d.put("name", h.getName());
                d.put("image", h.getImage());
                d.put("level", h.getLevel());
                d.put("price", h.getPrice());
                d.put("address", h.getAddress());
                d.put("phone", h.getPhone());
                d.put("facilities", h.getFacilities());
            });
            case "product" -> localProductRepo.findById(id).ifPresent(p -> {
                d.put("name", p.getName());
                d.put("image", p.getImage());
                d.put("category", p.getCategory());
                d.put("price", p.getPrice());
                d.put("description", p.getDescription());
                d.put("origin", p.getOrigin());
            });
            case "ticket" -> ticketRepo.findById(id).ifPresent(t -> {
                d.put("name", t.getName());
                d.put("image", t.getImage());
                d.put("price", t.getPrice());
                d.put("location", t.getLocation());
                d.put("description", t.getDescription());
                d.put("tags", splitTags(t.getTags()));
            });
        }
        return d.isEmpty() ? null : ChatResponse.CardData.builder().type(cardType).data(d).build();
    }

    /**
     * 从向量文档中提取所属景区/地点名称（用于卡片归属过滤）
     * 支持 attraction（relatedScenic）、food（relatedScenic）、ticket（location）
     */
    private String getEntityLocation(String collection, com.example.scenic_rag_system.dto.VectorDocument doc) {
        if (doc.getMetadata() == null || doc.getMetadata().get("id") == null) return null;
        try {
            Long id = Long.parseLong((String) doc.getMetadata().get("id"));
            return switch (collection) {
                case "attraction" -> attractionRepo.findById(id)
                        .map(a -> a.getRelatedScenic()).orElse(null);
                case "food" -> foodShopRepo.findById(id)
                        .map(f -> f.getRelatedScenic()).orElse(null);
                case "ticket" -> ticketRepo.findById(id)
                        .map(t -> t.getLocation()).orElse(null);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> splitTags(String tags) {
        return tags == null || tags.isBlank() ? List.of() : Arrays.stream(tags.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private String typeName(String t) {
        return switch (t) { case "attraction" -> "景点"; case "food" -> "美食"; case "hotel" -> "酒店"; case "product" -> "特产"; case "ticket" -> "门票"; default -> t; };
    }
}
