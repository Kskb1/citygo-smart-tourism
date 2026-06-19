package com.citygo.tourism.dto;

public record ProviderStatus(
        boolean amapMapConfigured,
        boolean amapWeatherConfigured,
        boolean amapPoiConfigured,
        boolean amapGeocodeConfigured,
        boolean amapRouteConfigured,
        boolean hotelPoiConfigured,
        boolean airportPoiConfigured,
        boolean liveHotelPriceAvailable,
        boolean liveFlightAvailable,
        String trainProvider,
        boolean trainThirdPartyConfigured
) {
}
