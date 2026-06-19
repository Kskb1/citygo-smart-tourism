package com.citygo.tourism.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "train")
public record TrainProperties(String provider, String thirdPartyKey) {
    public boolean thirdPartyConfigured() {
        return "third_party".equalsIgnoreCase(provider) && thirdPartyKey != null && !thirdPartyKey.isBlank();
    }
}
