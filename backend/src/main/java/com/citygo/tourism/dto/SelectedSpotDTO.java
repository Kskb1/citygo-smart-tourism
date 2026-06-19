package com.citygo.tourism.dto;

public record SelectedSpotDTO(
        String name,
        String cityName,
        String provinceName,
        String address,
        String type,
        Double longitude,
        Double latitude,
        String recommendationReason
) {
}
