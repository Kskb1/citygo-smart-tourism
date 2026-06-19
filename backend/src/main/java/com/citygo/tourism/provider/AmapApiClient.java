package com.citygo.tourism.provider;

import com.citygo.tourism.config.AmapProperties;
import com.citygo.tourism.dto.ApiResult;
import com.citygo.tourism.service.ApiLogService;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
class AmapApiClient {
    static final String SOURCE = "Amap Open Platform";
    private final AmapProperties properties;
    private final WebClient webClient;
    private final ApiLogService logService;

    AmapApiClient(AmapProperties properties, WebClient webClient, ApiLogService logService) {
        this.properties = properties;
        this.webClient = webClient;
        this.logService = logService;
    }

    ApiResult call(String apiName, Map<String, String> params, UriCustomizer customizer) {
        if (!properties.configured()) {
            String message = "AMAP_API_KEY 未配置，无法获取真实地图/天气/景点数据。";
            logService.log(apiName, params.toString(), false, message);
            return ApiResult.unavailable(SOURCE, message);
        }
        try {
            JsonNode json = webClient.get()
                    .uri(customizer::customize)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            boolean ok = json != null && "1".equals(json.path("status").asText());
            String error = ok ? null : json == null ? "高德接口未返回内容" : json.path("info").asText("高德接口调用失败");
            logService.log(apiName, params.toString(), ok, error);
            return ok ? ApiResult.real(SOURCE, json) : ApiResult.unavailable(SOURCE, error);
        } catch (Exception ex) {
            logService.log(apiName, params.toString(), false, ex.getMessage());
            return ApiResult.unavailable(SOURCE, "真实高德接口调用失败：" + ex.getMessage());
        }
    }

    String apiKey() {
        return properties.apiKey();
    }

    @FunctionalInterface
    interface UriCustomizer {
        URI customize(org.springframework.web.util.UriBuilder builder);
    }
}
