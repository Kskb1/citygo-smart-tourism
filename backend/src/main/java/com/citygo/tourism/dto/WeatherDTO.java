package com.citygo.tourism.dto;

public record WeatherDTO(
        String city,
        String province,
        String reportTime,
        String weather,
        String temperature,
        String windDirection,
        String windPower,
        String humidity,
        String travelTip
) {
}
