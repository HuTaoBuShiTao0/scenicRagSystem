package com.example.scenic_rag_system.common;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum KnowledgeType {
    SCENIC("scenic", "景区知识库", "景区"),
    ATTRACTION("attraction", "景点知识库", "景点"),
    FOOD("food", "美食知识库", "美食"),
    HOTEL("hotel", "酒店知识库", "酒店"),
    PRODUCT("product", "特产知识库", "特产"),
    TICKET("ticket", "门票知识库", "门票"),
    FIXED_QA("fixed_qa", "固定回答", "固定回答"),
    CUSTOMER_SERVICE("customer_service", "客服回答", "客服回答");

    private final String key;
    private final String label;
    private final String displayName;

    KnowledgeType(String key, String label, String displayName) {
        this.key = key;
        this.label = label;
        this.displayName = displayName;
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public String getDisplayName() { return displayName; }

    private static final Map<String, KnowledgeType> KEY_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(KnowledgeType::getKey, t -> t));

    public static KnowledgeType fromKey(String key) {
        KnowledgeType type = KEY_MAP.get(key);
        if (type == null) throw new IllegalArgumentException("Unknown knowledge type: " + key);
        return type;
    }
}
