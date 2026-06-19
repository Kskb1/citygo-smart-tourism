package com.citygo.tourism.dto;

import java.util.List;

public record FeasibilityDTO(
        boolean feasible,
        String level,
        int recommendedDays,
        List<String> warnings,
        List<String> suggestions
) {
}
