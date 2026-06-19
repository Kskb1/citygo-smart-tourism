package com.citygo.tourism.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amadeus")
public record AmadeusProperties(String clientId, String clientSecret, String env) {
    public boolean configured() {
        return usable(clientId, "你的Amadeus ClientId", "你的ClientId")
                && usable(clientSecret, "你的Amadeus ClientSecret", "你的ClientSecret");
    }

    private boolean usable(String value, String... placeholders) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        for (String placeholder : placeholders) {
            if (placeholder.equals(trimmed)) {
                return false;
            }
        }
        return true;
    }

    public String baseUrl() {
        return "production".equalsIgnoreCase(env) ? "https://api.amadeus.com" : "https://test.api.amadeus.com";
    }
}
