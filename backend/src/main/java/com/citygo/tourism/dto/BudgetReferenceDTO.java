package com.citygo.tourism.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record BudgetReferenceDTO(
        String priceMode,
        String ruleVersion,
        BigDecimal userBudget,
        BigDecimal estimatedMin,
        BigDecimal estimatedTypical,
        BigDecimal estimatedMax,
        BigDecimal gapToTypical,
        String budgetLevel,
        List<String> suggestions,
        List<PriceEstimateDTO> items,
        String notice,
        OffsetDateTime generatedAt
) {
}
