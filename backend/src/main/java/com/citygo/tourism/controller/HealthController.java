package com.citygo.tourism.controller;

import com.citygo.tourism.service.ProviderStatusService;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final JdbcTemplate jdbcTemplate;
    private final ProviderStatusService providerStatusService;

    public HealthController(JdbcTemplate jdbcTemplate, ProviderStatusService providerStatusService) {
        this.jdbcTemplate = jdbcTemplate;
        this.providerStatusService = providerStatusService;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean databaseUp = databaseUp();
        result.put("status", databaseUp ? "UP" : "DEGRADED");
        result.put("database", databaseUp ? "UP" : "DOWN");
        result.put("amapConfigured", providerStatusService.status().amapPoiConfigured());
        result.put("timestamp", OffsetDateTime.now().toString());
        return result;
    }

    private boolean databaseUp() {
        try {
            Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return value != null && value == 1;
        } catch (Exception e) {
            return false;
        }
    }
}
