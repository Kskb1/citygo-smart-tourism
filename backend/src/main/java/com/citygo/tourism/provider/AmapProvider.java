package com.citygo.tourism.provider;

import com.citygo.tourism.dto.ApiResult;
import com.citygo.tourism.dto.AirportDTO;
import com.citygo.tourism.dto.GeoLocationDTO;
import com.citygo.tourism.dto.HotelDTO;
import com.citygo.tourism.dto.RouteDTO;
import com.citygo.tourism.dto.RouteStepDTO;
import com.citygo.tourism.dto.SpotCardDTO;
import com.citygo.tourism.dto.SpotSearchData;
import com.citygo.tourism.dto.WeatherDTO;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AmapProvider {
    private final AmapPoiClient poiClient;
    private final AmapWeatherClient weatherClient;
    private final AmapRouteClient routeClient;
    private final AmapGeocodeClient geocodeClient;

    public AmapProvider(AmapPoiClient poiClient, AmapWeatherClient weatherClient,
                        AmapRouteClient routeClient, AmapGeocodeClient geocodeClient) {
        this.poiClient = poiClient;
        this.weatherClient = weatherClient;
        this.routeClient = routeClient;
        this.geocodeClient = geocodeClient;
    }

    public ApiResult searchPoi(String city, String keyword) {
        ApiResult result = poiClient.search(city, keyword);
        if (!result.realData() || result.rawJson() == null) {
            return result;
        }
        return ApiResult.real(result.sourceName(), parseSpotSearch(city, keyword, result), result.rawJson());
    }

    public ApiResult searchHotels(String city, String keyword, String reference, String center, Integer radius, Integer page, Integer pageSize) {
        String hotelKeyword = keyword == null || keyword.isBlank() ? "酒店" : keyword.trim();
        String location = center == null || center.isBlank() ? resolveReferenceLocation(reference, city) : center.trim();
        ApiResult result = location == null
                ? poiClient.search(city, hotelKeyword, safePage(page), safePageSize(pageSize))
                : poiClient.around(location, hotelKeyword, radius == null ? 3000 : radius, safePage(page), safePageSize(pageSize));
        if (!result.realData() || result.rawJson() == null) {
            return result;
        }
        return ApiResult.real(result.sourceName(), parseHotels(result.rawJson(), location), result.rawJson());
    }

    public ApiResult searchAirports(String city, String keyword) {
        String airportKeyword = keyword == null || keyword.isBlank() ? "机场" : keyword.trim();
        ApiResult result = poiClient.search(city, airportKeyword, 1, 20);
        if (!result.realData() || result.rawJson() == null) {
            return result;
        }
        return ApiResult.real(result.sourceName(), parseAirports(result.rawJson()), result.rawJson());
    }

    public ApiResult weather(String city) {
        ApiResult result = weatherClient.weather(city);
        if (!result.realData() || result.rawJson() == null) {
            return result;
        }
        return ApiResult.real(result.sourceName(), parseWeather(result.rawJson()), result.rawJson());
    }

    public ApiResult route(String origin, String destination) {
        return structuredRoute(routeClient.driving(origin, destination), origin, destination, origin, destination);
    }

    public ApiResult route(String origin, String destination, String mode, String city) {
        String resolvedOrigin = resolveRouteLocation(origin, city);
        String resolvedDestination = resolveRouteLocation(destination, city);
        if (resolvedOrigin == null || resolvedDestination == null) {
            return ApiResult.unavailable("Amap Open Platform", "地点经纬度解析失败，无法获取真实路线数据。");
        }
        ApiResult result;
        if ("walking".equalsIgnoreCase(mode)) {
            result = routeClient.walking(resolvedOrigin, resolvedDestination);
            return structuredRoute(result, resolvedOrigin, resolvedDestination, origin, destination);
        }
        if ("transit".equalsIgnoreCase(mode) || "bus".equalsIgnoreCase(mode)) {
            result = routeClient.transit(resolvedOrigin, resolvedDestination, city == null ? "" : city);
            return structuredRoute(result, resolvedOrigin, resolvedDestination, origin, destination);
        }
        result = routeClient.driving(resolvedOrigin, resolvedDestination);
        return structuredRoute(result, resolvedOrigin, resolvedDestination, origin, destination);
    }

    public ApiResult routeWithNames(String origin, String destination, String originName, String destinationName) {
        return structuredRoute(routeClient.driving(origin, destination), origin, destination, originName, destinationName);
    }

    public ApiResult geocode(String address, String city) {
        return geocodeClient.geocode(address, city);
    }

    private List<HotelDTO> parseHotels(JsonNode rawJson, String center) {
        List<HotelDTO> hotels = new ArrayList<>();
        JsonNode pois = rawJson.path("pois");
        if (pois.isArray()) {
            for (JsonNode poi : pois) {
                String location = text(poi, "location");
                Long distance = longValueObject(text(poi, "distance"));
                if (distance == null && center != null) {
                    distance = straightDistanceMeters(center, location);
                }
                hotels.add(new HotelDTO(
                        text(poi, "id"),
                        text(poi, "name"),
                        text(poi, "cityname"),
                        text(poi, "adname"),
                        text(poi, "address"),
                        new GeoLocationDTO(coordinate(location, 0), coordinate(location, 1)),
                        location,
                        text(poi, "type"),
                        text(poi, "tel", null),
                        distance,
                        distance == null ? null : distanceText(distance),
                        distance == null ? null : text(poi, "distance").isBlank() ? "直线距离" : "高德周边距离",
                        "高德开放平台",
                        "POI_ONLY",
                        true,
                        false,
                        "实时房价和房态请以酒店或正规预订平台为准。"));
            }
        }
        return hotels;
    }

    private List<AirportDTO> parseAirports(JsonNode rawJson) {
        List<AirportDTO> airports = new ArrayList<>();
        JsonNode pois = rawJson.path("pois");
        if (pois.isArray()) {
            for (JsonNode poi : pois) {
                String location = text(poi, "location");
                airports.add(new AirportDTO(
                        text(poi, "id"),
                        text(poi, "name"),
                        text(poi, "cityname"),
                        text(poi, "adname"),
                        text(poi, "address"),
                        new GeoLocationDTO(coordinate(location, 0), coordinate(location, 1)),
                        location,
                        text(poi, "type"),
                        text(poi, "tel", null),
                        "高德开放平台",
                        "POI_ONLY",
                        true,
                        false,
                        "高德提供机场位置和周边地面路线；航班、票价和余票请以航空公司或正规平台为准。"));
            }
        }
        return airports;
    }

    private String resolveReferenceLocation(String reference, String city) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        ApiResult geocode = geocode(reference.trim(), city == null ? "" : city);
        String location = firstGeocodeLocation(geocode);
        if (location != null) {
            return location;
        }
        ApiResult poi = poiClient.search(city == null ? "" : city, reference.trim(), 1, 1);
        if (poi.realData() && poi.rawJson() != null && poi.rawJson().path("pois").isArray() && !poi.rawJson().path("pois").isEmpty()) {
            return text(poi.rawJson().path("pois").get(0), "location", null);
        }
        return null;
    }

    private String resolveRouteLocation(String value, String city) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (isCoordinate(trimmed)) {
            return trimmed;
        }
        return resolveReferenceLocation(trimmed, city);
    }

    private boolean isCoordinate(String value) {
        if (value == null) {
            return false;
        }
        String[] parts = value.split(",");
        if (parts.length != 2) {
            return false;
        }
        try {
            Double.parseDouble(parts[0]);
            Double.parseDouble(parts[1]);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String firstGeocodeLocation(ApiResult result) {
        if (result == null || !result.realData() || result.rawJson() == null) {
            return null;
        }
        JsonNode geocodes = result.rawJson().path("geocodes");
        if (!geocodes.isArray() || geocodes.isEmpty()) {
            return null;
        }
        return text(geocodes.get(0), "location", null);
    }

    private ApiResult structuredRoute(ApiResult result, String origin, String destination, String originName, String destinationName) {
        if (!result.realData() || result.rawJson() == null) {
            return result;
        }
        return ApiResult.real(result.sourceName(), parseRoute(result.rawJson(), origin, destination, originName, destinationName), result.rawJson());
    }

    private SpotSearchData parseSpotSearch(String city, String keyword, ApiResult result) {
        List<SpotCardDTO> spots = new ArrayList<>();
        JsonNode pois = result.rawJson().path("pois");
        String fetchedAt = result.fetchedAt().toString();
        if (pois.isArray()) {
            for (JsonNode poi : pois) {
                String location = text(poi, "location");
                Double longitude = coordinate(location, 0);
                Double latitude = coordinate(location, 1);
                String type = text(poi, "type");
                spots.add(new SpotCardDTO(
                        text(poi, "id"),
                        text(poi, "name"),
                        text(poi, "address"),
                        type,
                        text(poi, "cityname", city),
                        location,
                        longitude,
                        latitude,
                        firstPhoto(poi),
                        text(poi, "distance"),
                        recommendationReason(type),
                        result.sourceName(),
                        fetchedAt,
                        true));
            }
        }
        return new SpotSearchData(city, keyword, spots);
    }

    private WeatherDTO parseWeather(JsonNode rawJson) {
        JsonNode lives = rawJson.path("lives");
        if (lives.isArray() && !lives.isEmpty()) {
            JsonNode weather = lives.get(0);
            String weatherText = text(weather, "weather");
            String temperature = text(weather, "temperature");
            return new WeatherDTO(
                    text(weather, "city"),
                    text(weather, "province"),
                    text(weather, "reporttime"),
                    weatherText,
                    temperature,
                    text(weather, "winddirection"),
                    text(weather, "windpower"),
                    text(weather, "humidity"),
                    travelTip(weatherText, temperature));
        }
        JsonNode forecast = rawJson.path("forecasts").isArray() && !rawJson.path("forecasts").isEmpty()
                ? rawJson.path("forecasts").get(0)
                : rawJson;
        JsonNode cast = forecast.path("casts").isArray() && !forecast.path("casts").isEmpty()
                ? forecast.path("casts").get(0)
                : forecast;
        String weatherText = text(cast, "dayweather");
        String temperature = text(cast, "daytemp");
        return new WeatherDTO(
                text(forecast, "city"),
                text(forecast, "province"),
                text(forecast, "reporttime"),
                weatherText,
                temperature,
                text(cast, "daywind"),
                text(cast, "daypower"),
                text(cast, "humidity"),
                travelTip(weatherText, temperature));
    }

    private RouteDTO parseRoute(JsonNode rawJson, String origin, String destination, String originName, String destinationName) {
        JsonNode route = rawJson.path("route");
        JsonNode path = route.path("paths").isArray() && !route.path("paths").isEmpty()
                ? route.path("paths").get(0)
                : route.path("transits").isArray() && !route.path("transits").isEmpty() ? route.path("transits").get(0) : route;
        List<RouteStepDTO> steps = new ArrayList<>();
        List<String> polylines = new ArrayList<>();
        JsonNode rawSteps = path.path("steps");
        if (rawSteps.isArray()) {
            for (JsonNode step : rawSteps) {
                steps.add(new RouteStepDTO(text(step, "instruction"), text(step, "road"), text(step, "distance"), text(step, "duration")));
                addPolyline(polylines, text(step, "polyline"));
            }
        }
        long distance = longValue(path.path("distance").asText(null));
        long duration = longValue(path.path("duration").asText(null));
        return new RouteDTO(
                originName,
                destinationName,
                origin,
                destination,
                distance,
                distanceText(distance),
                duration,
                durationText(duration),
                text(path, "strategy"),
                steps,
                polylines,
                "高德开放平台",
                "LIVE_ROUTE",
                true);
    }

    private String firstPhoto(JsonNode poi) {
        JsonNode photos = poi.path("photos");
        if (photos.isArray() && !photos.isEmpty()) {
            return text(photos.get(0), "url");
        }
        return "";
    }

    private String recommendationReason(String type) {
        if (type == null) {
            return "来自真实 POI 数据，适合作为目的地游玩参考。";
        }
        if (type.contains("风景名胜")) {
            return "该地点属于风景名胜，适合观光游览。";
        }
        if (type.contains("博物馆")) {
            return "该地点适合历史文化体验。";
        }
        if (type.contains("公园")) {
            return "该地点适合休闲散步和轻松游览。";
        }
        if (type.contains("餐饮") || type.contains("美食")) {
            return "该地点适合美食体验。";
        }
        return "该地点来自真实 POI 数据，适合作为行程备选。";
    }

    private String travelTip(String weather, String temperature) {
        String text = weather == null ? "" : weather;
        int temp = (int) longValue(temperature);
        if (text.contains("雨")) {
            return "建议携带雨具，优先安排室内景点或交通便利的路线。";
        }
        if (temp >= 32) {
            return "天气较热，建议避开中午长时间户外游玩。";
        }
        if (text.contains("晴")) {
            return "天气适合户外景点，注意防晒和补水。";
        }
        return "建议根据实时天气调整户外和室内景点顺序。";
    }

    private void addPolyline(List<String> target, String polyline) {
        if (polyline == null || polyline.isBlank()) {
            return;
        }
        for (String point : polyline.split(";")) {
            if (!point.isBlank()) {
                target.add(point);
            }
        }
    }

    private String distanceText(long meters) {
        if (meters <= 0) {
            return "暂无距离数据";
        }
        if (meters >= 1000) {
            return String.format("%.1f 公里", meters / 1000.0);
        }
        return meters + " 米";
    }

    private String durationText(long seconds) {
        if (seconds <= 0) {
            return "暂无耗时数据";
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + " 小时 " + minutes + " 分钟";
        }
        return minutes + " 分钟";
    }

    private String text(JsonNode node, String field) {
        return text(node, field, "");
    }

    private int safePage(Integer page) {
        return page == null ? 1 : Math.max(1, page);
    }

    private int safePageSize(Integer pageSize) {
        return pageSize == null ? 20 : Math.min(Math.max(1, pageSize), 25);
    }

    private String text(JsonNode node, String field, String fallback) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return fallback;
        }
        String value = node.path(field).asText("");
        return value == null || "[]".equals(value) ? fallback : value;
    }

    private Double coordinate(String location, int index) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String[] parts = location.split(",");
        if (parts.length <= index) {
            return null;
        }
        try {
            return Double.parseDouble(parts[index]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private long longValue(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private Long longValueObject(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long straightDistanceMeters(String from, String to) {
        Double lng1 = coordinate(from, 0);
        Double lat1 = coordinate(from, 1);
        Double lng2 = coordinate(to, 0);
        Double lat2 = coordinate(to, 1);
        if (lng1 == null || lat1 == null || lng2 == null || lat2 == null) {
            return null;
        }
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(earthRadius * c);
    }
}
