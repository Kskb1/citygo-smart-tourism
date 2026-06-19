package com.citygo.tourism.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserTripDTO(
        Long id,
        Long userId,
        String username,
        String title,
        String fromCity,
        String toCity,
        LocalDate startDate,
        Integer days,
        Integer peopleCount,
        BigDecimal budget,
        String feasibilityLevel,
        Integer recommendedDays,
        String summary,
        String priceMode,
        String priceRuleVersion,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        JsonNode planData
) {
}
