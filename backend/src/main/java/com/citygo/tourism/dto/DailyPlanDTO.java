package com.citygo.tourism.dto;

import java.util.List;

public record DailyPlanDTO(
        int day,
        String date,
        String title,
        List<PlanActivityDTO> activities
) {
}
