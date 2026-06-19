package com.citygo.tourism.dto;

import java.math.BigDecimal;

public record TransportPlanDTO(
        String routeName,
        String status,
        String message,
        String officialTrainQueryUrl,
        String officialTrainQueryTip,
        boolean realPriceAvailable,
        BigDecimal price
) {
}
