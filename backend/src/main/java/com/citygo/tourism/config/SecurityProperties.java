package com.citygo.tourism.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "citygo.security")
public record SecurityProperties(
        String jwtSecret,
        long tokenTtlHours,
        String adminUsername,
        String adminPassword,
        String adminEmail,
        boolean demoUsersEnabled
) {
    public String effectiveSecret() {
        return jwtSecret == null || jwtSecret.isBlank() ? "dev-only-change-me-citygo-secret" : jwtSecret;
    }

    public long effectiveTtlHours() {
        return tokenTtlHours <= 0 ? 24 : tokenTtlHours;
    }

    public String effectiveAdminUsername() {
        return adminUsername == null || adminUsername.isBlank() ? "admin" : adminUsername.trim();
    }

    public String effectiveAdminEmail() {
        return adminEmail == null || adminEmail.isBlank() ? "admin@citygo.local" : adminEmail.trim();
    }
}
