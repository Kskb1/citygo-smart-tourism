package com.citygo.tourism.dto;

public record TrainSearchResponse(
        boolean realData,
        String provider,
        String message,
        String officialUrl
) {
}
