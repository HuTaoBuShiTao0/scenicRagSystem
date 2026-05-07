package com.example.scenic_rag_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {
    /** 意图类型：景点讲解/美食导购/酒店导购/特产文创导购/天气查询/门票购买/门票订单查看/客服问答/拒绝回答 */
    private String intentType;

    /** 参数列表 */
    @Builder.Default
    private Map<String, String> params = new HashMap<>();
}
