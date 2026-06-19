package com.citygo.tourism.dto;

public record RouteStepDTO(
        String instruction,
        String road,
        String distance,
        String duration
) {
}
