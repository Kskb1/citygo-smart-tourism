package com.citygo.tourism.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SaveTripRequest(
        @NotBlank String title,
        @NotBlank String fromCity,
        @NotBlank String toCity,
        @NotNull LocalDate startDate,
        @NotNull Integer days,
        @NotNull Integer peopleCount,
        Integer roomCount,
        BigDecimal budget,
        String feasibilityLevel,
        Integer recommendedDays,
        String summary,
        String priceMode,
        String priceRuleVersion,
        @NotNull JsonNode planData
) {
}
