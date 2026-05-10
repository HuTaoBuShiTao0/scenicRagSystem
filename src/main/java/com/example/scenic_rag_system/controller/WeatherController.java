package com.example.scenic_rag_system.controller;

import com.example.scenic_rag_system.common.Result;
import com.example.scenic_rag_system.dto.WeatherResult;
import com.example.scenic_rag_system.service.QWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 天气数据接口（供前端组件调用）
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final QWeatherService qWeatherService;

    @GetMapping("/today")
    public Result<?> getTodayWeather(@RequestParam(defaultValue = "洛阳") String location) {
        try {
            List<WeatherResult> forecasts = qWeatherService.getWeatherForecast(location, "3d");
            if (forecasts != null && !forecasts.isEmpty()) {
                return Result.success(forecasts.get(0));
            }
            return Result.error("暂无天气数据");
        } catch (Exception e) {
            return Result.error("获取天气失败: " + e.getMessage());
        }
    }
}
