package com.citygo.tourism.dto;

public record SpotCardDTO(
        String id,
        String name,
        String address,
        String type,
        String cityName,
        String location,
        Double longitude,
        Double latitude,
        String photoUrl,
        String distance,
        String recommendationReason,
        String sourceName,
        String fetchedAt,
        boolean realData
) {
}
