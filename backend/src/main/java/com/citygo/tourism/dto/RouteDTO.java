package com.citygo.tourism.dto;

import java.util.List;

public record RouteDTO(
        String originName,
        String destinationName,
        String originLocation,
        String destinationLocation,
        Long distanceMeters,
        String distanceText,
        Long durationSeconds,
        String durationText,
        String strategy,
        List<RouteStepDTO> steps,
        List<String> polylinePoints,
        String sourceName,
        String dataMode,
        boolean realData
) {
}
