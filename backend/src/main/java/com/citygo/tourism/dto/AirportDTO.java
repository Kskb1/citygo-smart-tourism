package com.citygo.tourism.dto;

public record AirportDTO(
        String id,
        String name,
        String cityName,
        String district,
        String address,
        GeoLocationDTO location,
        String locationText,
        String typeName,
        String telephone,
        String sourceName,
        String dataMode,
        boolean realData,
        boolean liveFlightAvailable,
        String notice
) {
}
