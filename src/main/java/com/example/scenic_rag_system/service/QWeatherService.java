package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.common.QWeatherJwtUtil;
import com.example.scenic_rag_system.dto.WeatherResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 和风天气服务
 * 使用 JWT (EdDSA) 认证，调和风天气每日天气预报 API
 */
@Service
@Slf4j
public class QWeatherService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${qweather.key-id}")
    private String keyId;

    @Value("${qweather.project-id}")
    private String projectId;

    @Value("${qweather.private-key}")
    private String privateKey;

    @Value("${qweather.api-host}")
    private String apiHost;

    /** JWT 令牌缓存 */
    private String cachedToken = "";
    private long tokenExpiry = 0;

    /** 常用城市名称到 LocationID 的映射（GeoAPI 不可用时的保底方案） */
    private static final Map<String, String> CITY_ID_MAP = Map.ofEntries(
        Map.entry("洛阳", "101180901"),
        Map.entry("北京", "101010100"),
        Map.entry("上海", "101020100"),
        Map.entry("广州", "101280101"),
        Map.entry("深圳", "101280601"),
        Map.entry("杭州", "101210101"),
        Map.entry("南京", "101190101"),
        Map.entry("成都", "101270101"),
        Map.entry("武汉", "101200101"),
        Map.entry("西安", "101110101"),
        Map.entry("郑州", "101180101"),
        Map.entry("开封", "101180801"),
        Map.entry("苏州", "101190401"),
        Map.entry("长沙", "101250101"),
        Map.entry("重庆", "101040100"),
        Map.entry("天津", "101030100"),
        Map.entry("青岛", "101120201"),
        Map.entry("厦门", "101230201"),
        Map.entry("昆明", "101290101"),
        Map.entry("大连", "101070201")
    );

    public QWeatherService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    /**
     * 获取缓存的 JWT 令牌，过期自动续签
     */
    private String getToken() {
        if (cachedToken.isEmpty() || Instant.now().getEpochSecond() >= tokenExpiry - 60) {
            cachedToken = QWeatherJwtUtil.generateToken(keyId, projectId, privateKey);
            tokenExpiry = Instant.now().getEpochSecond() + 840;
            log.info("QWeather JWT token refreshed, length={}", cachedToken.length());
        }
        return cachedToken;
    }

    /**
     * 查询城市 LocationID
     * 优先级：自定义 API host > 标准 geoapi > 城市名称映射表 > 直接返回城市名
     */
    public String lookupLocation(String cityName) {
        // 先查预置映射表（最快最稳）
        String mappedId = CITY_ID_MAP.get(cityName);
        if (mappedId != null) {
            log.info("QWeather using preset city ID for '{}': {}", cityName, mappedId);
            return mappedId;
        }

        // 尝试自定义 host 的 geo 接口
        String customHostUrl = "https://" + apiHost + "/v2/city/lookup?location=" + cityName;
        String id = doGeoLookup(customHostUrl, cityName);
        if (id != null) return id;

        // 尝试标准 geoapi host
        String standardUrl = "https://geoapi.qweather.com/v2/city/lookup?location=" + cityName;
        id = doGeoLookup(standardUrl, cityName);
        if (id != null) return id;

        // 都不行，直接用城市名（API 可能支持）
        log.warn("QWeather all geo lookups failed for '{}', using name directly", cityName);
        return cityName;
    }

    /**
     * 执行一次 GeoAPI 查询
     */
    private String doGeoLookup(String url, String cityName) {
        try {
            log.info("QWeather geo lookup: {}", url);
            String response = webClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + getToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                log.warn("QWeather geo lookup returned null for: {}", url);
                return null;
            }

            JsonNode root = objectMapper.readTree(response);
            String code = root.path("code").asText();
            log.info("QWeather geo response code={} for city={}", code, cityName);

            if (!"200".equals(code)) return null;

            JsonNode locationList = root.path("location");
            if (locationList.isArray() && locationList.size() > 0) {
                String locationId = locationList.get(0).path("id").asText();
                log.info("QWeather resolved '{}' -> LocationID={}", cityName, locationId);
                return locationId;
            }
        } catch (Exception e) {
            log.warn("QWeather geo lookup error for {}: {}", url, e.getMessage());
        }
        return null;
    }

    /**
     * 查询指定城市的每日天气预报
     *
     * @param locationName 城市名称，如"洛阳"
     * @param days         预报天数，如 "3d" / "7d"
     * @return 天气预报结果列表
     */
    public List<WeatherResult> getWeatherForecast(String locationName, String days) {
        try {
            String locationId = lookupLocation(locationName);
            String url = "https://" + apiHost + "/v7/weather/" + days + "?location=" + locationId;
            log.info("QWeather forecast request: {}", url);

            String response = webClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + getToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                log.warn("QWeather forecast returned null for: {}", locationName);
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response);
            String code = root.path("code").asText();
            log.info("QWeather forecast response code={} for location={}", code, locationName);

            if (!"200".equals(code)) {
                log.warn("QWeather forecast failed, code={}, response={}", code,
                        response.length() > 200 ? response.substring(0, 200) : response);
                return Collections.emptyList();
            }

            JsonNode dailyArray = root.path("daily");
            if (!dailyArray.isArray()) return Collections.emptyList();

            List<WeatherResult> results = StreamSupport.stream(dailyArray.spliterator(), false)
                    .map(day -> WeatherResult.builder()
                            .location(locationName)
                            .fxDate(day.path("fxDate").asText())
                            .tempMax(day.path("tempMax").asText())
                            .tempMin(day.path("tempMin").asText())
                            .textDay(day.path("textDay").asText())
                            .textNight(day.path("textNight").asText())
                            .windDirDay(day.path("windDirDay").asText())
                            .windScaleDay(day.path("windScaleDay").asText())
                            .humidity(day.path("humidity").asText())
                            .pressure(day.path("pressure").asText())
                            .vis(day.path("vis").asText())
                            .uvIndex(day.path("uvIndex").asText())
                            .precip(day.path("precip").asText())
                            .build())
                    .collect(Collectors.toList());
            log.info("QWeather fetched {} days forecast for {}", results.size(), locationName);
            return results;
        } catch (Exception e) {
            log.error("QWeather forecast error for '{}': {}", locationName, e.getMessage());
            return Collections.emptyList();
        }
    }
}
