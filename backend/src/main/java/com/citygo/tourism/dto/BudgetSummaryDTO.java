package com.citygo.tourism.dto;

import java.math.BigDecimal;
import java.util.List;

public record BudgetSummaryDTO(
        BigDecimal userBudget,
        BigDecimal knownRealCost,
        BigDecimal estimatedCost,
        BigDecimal totalMinCost,
        BigDecimal remainingBudget,
        BigDecimal foodEstimate,
        BigDecimal localTrafficEstimate,
        BigDecimal hotelEstimate,
        String ticketEstimate,
        BigDecimal transportRealCost,
        String transportStatus,
        BudgetReferenceDTO budgetReference,
        List<String> warnings
) {
}
