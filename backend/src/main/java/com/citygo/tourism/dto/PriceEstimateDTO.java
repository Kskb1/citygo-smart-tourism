package com.citygo.tourism.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PriceEstimateDTO(
        String category,
        String priceMode,
        String currency,
        BigDecimal minPrice,
        BigDecimal typicalPrice,
        BigDecimal maxPrice,
        String unit,
        List<String> basis,
        String confidence,
        String sourceName,
        String notice,
        OffsetDateTime generatedAt,
        String ruleVersion
) {
}
