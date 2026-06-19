package com.citygo.tourism.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTripTitleRequest(@NotBlank String title) {
}
