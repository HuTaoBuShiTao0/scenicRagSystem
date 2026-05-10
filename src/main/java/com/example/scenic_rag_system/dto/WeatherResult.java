package com.example.scenic_rag_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 和风天气查询结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResult {
    /** 城市名称 */
    private String location;
    /** 预报日期 */
    private String fxDate;
    /** 最高温度 */
    private String tempMax;
    /** 最低温度 */
    private String tempMin;
    /** 白天天气描述 */
    private String textDay;
    /** 夜间天气描述 */
    private String textNight;
    /** 白天风向 */
    private String windDirDay;
    /** 白天风力等级 */
    private String windScaleDay;
    /** 相对湿度 */
    private String humidity;
    /** 大气压强 */
    private String pressure;
    /** 能见度 */
    private String vis;
    /** 紫外线指数 */
    private String uvIndex;
    /** 总降水量 */
    private String precip;
}
