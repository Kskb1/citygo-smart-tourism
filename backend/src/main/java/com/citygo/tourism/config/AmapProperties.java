package com.citygo.tourism.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amap")
public record AmapProperties(String apiKey) {
    public boolean configured() {
        return apiKey != null
                && !apiKey.isBlank()
                && !"你的高德开放平台Web服务Key".equals(apiKey.trim())
                && !"你的高德Key".equals(apiKey.trim());
    }
}
