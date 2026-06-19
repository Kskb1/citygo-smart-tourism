package com.citygo.tourism.dto;

public record PlanActivityDTO(
        String period,
        String title,
        String description,
        String reason,
        String costNote
) {
}
