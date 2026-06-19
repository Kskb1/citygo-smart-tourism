package com.citygo.tourism.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "citygo.estimate")
public record PriceEstimateProperties(
        String ruleVersion,
        BigDecimal weekendFactor,
        BigDecimal shortNoticeFactor,
        BigDecimal normalNoticeFactor,
        BigDecimal emergencyReserveRate,
        Flight flight,
        Train train,
        Hotel hotel,
        Food food,
        LocalTransport localTransport
) {
    public String version() {
        return ruleVersion == null || ruleVersion.isBlank() ? "1.0" : ruleVersion;
    }

    public BigDecimal factorOrOne(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }

    public record Flight(Map<String, List<BigDecimal>> distanceBands) {
    }

    public record Train(
            BigDecimal highSpeedSecondClassRate,
            BigDecimal highSpeedFirstClassRate,
            BigDecimal normalHardSeatRate,
            BigDecimal normalHardSleeperRate
    ) {
    }

    public record Hotel(
            Map<String, List<String>> cityTiers,
            Map<String, Map<String, List<BigDecimal>>> tiers,
            Map<String, List<BigDecimal>> fallback
    ) {
    }

    public record Food(Map<String, List<BigDecimal>> levels) {
    }

    public record LocalTransport(Map<String, List<BigDecimal>> levels) {
    }
}
