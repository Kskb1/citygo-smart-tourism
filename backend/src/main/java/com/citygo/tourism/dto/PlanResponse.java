package com.citygo.tourism.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PlanResponse(
        String title,
        String summary,
        String fromCity,
        String toCity,
        String startDate,
        int days,
        int peopleCount,
        BigDecimal budget,
        int recommendedDays,
        String budgetConclusion,
        FeasibilityDTO feasibility,
        List<CitySegmentDTO> citySegments,
        TransportPlanDTO outboundTransport,
        TransportPlanDTO returnTransport,
        WeatherDTO weatherSummary,
        List<DailyPlanDTO> dailyPlans,
        List<SpotCardDTO> recommendedSpots,
        RouteDTO routeOverview,
        BudgetSummaryDTO budgetSummary,
        List<String> dataWarnings,
        List<DataSourceDTO> dataSources,
        Map<String, ApiResult> debugRawData,
        PlanGenerateRequest request,
        List<String> notices,
        Map<String, ApiResult> data,
        TrainSearchResponse train,
        BudgetEstimate budgetEstimate
) {
    public record BudgetEstimate(
            BigDecimal flightOrTrainCost,
            BigDecimal hotelCost,
            BigDecimal foodEstimate,
            BigDecimal localTrafficEstimate,
            BigDecimal totalKnownAndEstimated,
            List<String> notes
    ) {
    }
}
