package com.citygo.tourism.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "citygo")
public record CityGoProperties(Integer httpTimeoutSeconds) {
    public int timeoutSeconds() {
        return httpTimeoutSeconds == null ? 15 : httpTimeoutSeconds;
    }
}
