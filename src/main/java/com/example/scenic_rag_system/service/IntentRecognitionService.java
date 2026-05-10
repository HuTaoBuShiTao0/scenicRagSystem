package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.controller.PromptController;
import com.example.scenic_rag_system.dto.IntentResult;
import com.example.scenic_rag_system.entity.Prompt;
import com.example.scenic_rag_system.repository.PromptRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 意图识别服务
 * 使用智谱LLM根据预设提示词识别用户意图
 * 提示词内容优先从数据库加载（支持前端实时编辑），无配置时使用内置默认值
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IntentRecognitionService {

    private final ZhipuApiService zhipuApiService;
    private final ObjectMapper objectMapper;
    private final PromptRepository promptRepository;

    private static final String PROMPT_TYPE = "INTENT_RECOGNITION";

    private static final String FALLBACK_SYSTEM_PROMPT = """
            你是一个旅游意图识别助手，擅长结合对话上下文对用户提问准确识别旅行相关意图。

            ## 技能

            ### 技能1. 结合对话上下文理解最新Query
            你的任务是在结合对话历史的基础上，理解用户的最新提问，使问题信息完整合理。具体要求如下：
            1. 如果最新Query与对话上下文无关，忽略上下文。
            2. 当最新Query和对话上下文中提及了多个景点或商品时，优先取最新的景点或商品；若缺少相关信息，则使用对话上下文中的最新信息补充。

            ### 技能2. 旅行相关的意图识别
            你的任务是结合对话上下文，识别最新Query的意图类型，并抽取相应的参数。在以下意图类型中进行分类：
            '''
            1. 景点讲解 
            描述: 用户的提问涉及到景点的介绍、有哪些特色景点、附近好玩的地方推荐、热门景区/景点推荐
            参数:
            - 景点: 提问中的景点名，如果有多个只提取一个。
            - 标签: 提问中的关键词,多个关键词用英文逗号','分隔开，如自然,古树,文物,热门,特色,冷门，推荐等，如没有可为空，不可捏造，比如问"镇江市有哪些湿地观光景点"提取标签为"镇江市,湿地观光"。
            - 问题类型: 固定景点介绍/附近景点介绍，默认是固定景点介绍。
            - 出发地: 如果问题类型是附近景点介绍，需要提取出发地的地名,如城山广场附近的景点推荐，提取出发地为城山广场，如没有指定则为空。
            
            2. 美食导购 
            描述: 咨询旅行相关的美食、餐厅以及本地特色小吃，涉及"吃"相关的信息。
            参数: 
            - 问题类型: 附近推荐/非附近推荐，提问包含"附近","旁边","周边"相关的或者有明确出发地的是附近推荐，否则是非附近推荐。
            - 出发地: 提问中的出发地的地名,如"万岁山有哪些美食"，提取出发地为"万岁山武侠城"，"品尝苏州美食"，提取出发地为"苏州"。
            - 标签: 提问中的关键词,多个标签名用英文逗号','分割开，没有明确默认为空。

            3. 酒店导购 
            描述: 咨询旅行相关的酒店
            参数: 
            - 问题类型: 附近推荐/非附近推荐，提问包含"附近","周边","旁边"相关有什么酒店是附近推荐，否则是非附近推荐，比如问有什么便宜的酒店
            - 出发地: 用户提问中的出发地地名，比如"苏州有哪些酒店"，提取出发地为"苏州"。
            - 标签: 用户提问中的关键词,多个标签名用英文逗号','分割开，如没有明确，可为空。

            4. 特产文创导购
            描述: 咨询旅行相关的特产、伴手礼、文创、日常生活用品等商品
            参数:
            - 问题类型: 附近推荐/非附近推荐，提问包含"附近","周边","旁边"相关有什么特产是附近推荐，否则是非附近推荐
            - 出发地: 提问中的出发地的地名,如杭州湘湖附近的特产推荐，提取出发地为杭州湘湖
            - 标签: 提问中的关键词,多个标签名用英文逗号','分割开，如果没有明确，可为空。

            5. 天气查询 
            - 描述: 咨询天气、气温、温度。如"湘湖今天热么？"
            - 参数: 
              - 地点: 默认为空提问里的地点名，解析成地点所在市区县名，没明确或不在中国或不是有效区县名时为空，例如"火星上天气"直接返回空
              - 时间: 用户想查询哪天的天气，如："今天"，"明天"，"后天"，"最近几天"，"这周六"，"这周末"，"下周一"等。如果咨询的是具体日期如"25号"或者"25号-27号" "未来3天"，则需要根据日期解析成 "xxxx-xx-xx 00:00:00" 或者 "xxxx-xx-xx 00:00:00->xxxx-xx-xx 23:59:59"，注意必须是yyyy-MM-dd HH:mm:ss格式。

            6. 门票购买
            - 描述: 用户提问关于景点门票和演出票相关的购票方式、预定方式相关问题时，意图为门票购买
            - 参数:
              - 地点: 用户提问中景点信息和地理位置信息，如西湖的门票推荐，提取为西湖
              - 名称: 用户提问中的门票的名称，提取不到则为空。
              - 价格: 价格区间，如"15.1->50.1"
              - 标签: 关键词标签，多个标签名用英文逗号","分割开
              - 问题类型: 附近推荐/非附近推荐

            7. 门票订单查看
            - 描述: 用户提问关于查询已购买门票订单相关的问题
            - 参数:
              - 实体: 门票/订单
              - 标签: 关键词标签
              - 景点: 归属景点

            8. 客服问答
            - 描述: 无法进入上述分类，咨询门票范围、当地新闻、停车咨询、野史、景区地图、舆情投诉、活动攻略、景区开放时间，景点是否收费，失物招领，请求人工帮助等
            - 参数: 空

            9. 拒绝回答
            - 描述: 最新Query包含暴力、色情、赌博、政治敏感等非法信息，或不含中文文本，意义不明
            - 参数: 空
            '''

            ### 输出格式
            输出结果应遵循以下 JSON 格式，不要输出额外内容：
            {"意图类型":"<意图类型>","参数列表":{"参数1":"参数1的取值","参数n":"参数n的取值"}}
            """;

    /**
     * 从数据库或缓存获取意图识别提示词，不存在则使用内置默认值
     */
    private String getSystemPrompt() {
        // 先从缓存读取
        String cached = PromptController.promptCache.get(PROMPT_TYPE);
        if (cached != null) {
            return cached;
        }
        // 从数据库加载
        try {
            Optional<Prompt> promptOpt = promptRepository.findByType(PROMPT_TYPE);
            if (promptOpt.isPresent() && promptOpt.get().getEnabled()) {
                String content = promptOpt.get().getContent();
                PromptController.promptCache.put(PROMPT_TYPE, content);
                return content;
            }
        } catch (Exception e) {
            log.warn("Failed to load prompt from DB, using fallback: {}", e.getMessage());
        }
        return FALLBACK_SYSTEM_PROMPT;
    }

    /**
     * 识别用户意图
     */
    public IntentResult recognize(String userMessage, List<Map<String, String>> history) {
        try {
            // 构建对话上下文
            StringBuilder historyText = new StringBuilder();
            if (history != null && !history.isEmpty()) {
                for (Map<String, String> msg : history) {
                    historyText.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
                }
            }

            String systemPrompt = getSystemPrompt();

            // 替换提示词中的模板变量
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String userPrompt = systemPrompt + "\n\n"
                    + "今天时间日期是" + currentTime + "，根据<历史提问>和用户的<最新提问>进行意图类型列表输出。"
                    + "切记输出严格遵守示例的 JSON 格式，不输出额外内容:\n"
                    + "## 历史提问\n" + historyText + "\n\n"
                    + "最新提问:\n" + userMessage;

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));

            String response = zhipuApiService.chatSync(messages);
            log.debug("Intent recognition response: {}", response);

            return parseIntentResponse(response);
        } catch (Exception e) {
            log.error("Intent recognition failed: {}", e.getMessage());
            return IntentResult.builder()
                    .intentType("客服问答")
                    .params(new HashMap<>())
                    .build();
        }
    }

    private IntentResult parseIntentResponse(String json) {
        try {
            // 提取JSON部分（LLM可能输出多余内容）
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }

            Map<String, Object> result = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});

            String intentType = (String) result.getOrDefault("意图类型", "客服问答");
            Object paramsObj = result.get("参数列表");

            Map<String, String> params = new HashMap<>();
            if (paramsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paramMap = (Map<String, Object>) paramsObj;
                paramMap.forEach((k, v) -> params.put(k, v != null ? v.toString() : ""));
            }

            return IntentResult.builder()
                    .intentType(intentType)
                    .params(params)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse intent response: {}", e.getMessage());
            return IntentResult.builder()
                    .intentType("客服问答")
                    .params(new HashMap<>())
                    .build();
        }
    }
}
