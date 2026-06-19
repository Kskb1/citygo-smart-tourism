package com.citygo.tourism.dto;

public record HotelDTO(
        String id,
        String name,
        String cityName,
        String district,
        String address,
        GeoLocationDTO location,
        String locationText,
        String typeName,
        String telephone,
        Long distanceMeters,
        String distanceText,
        String distanceMode,
        String sourceName,
        String dataMode,
        boolean realData,
        boolean livePriceAvailable,
        String notice
) {
}
