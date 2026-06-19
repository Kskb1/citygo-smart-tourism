package com.citygo.tourism.provider;

import com.citygo.tourism.dto.ApiResult;
import com.citygo.tourism.service.ApiLogService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AmadeusHotelClient {
    private static final String SOURCE = "Amadeus Hotel APIs";
    private final AmadeusAuthClient authClient;
    private final IataCodeBook iataCodeBook;
    private final WebClient webClient;
    private final ApiLogService logService;

    public AmadeusHotelClient(AmadeusAuthClient authClient, IataCodeBook iataCodeBook, WebClient webClient, ApiLogService logService) {
        this.authClient = authClient;
        this.iataCodeBook = iataCodeBook;
        this.webClient = webClient;
        this.logService = logService;
    }

    public ApiResult search(String city, LocalDate checkIn, LocalDate checkOut) {
        Map<String, String> params = Map.of("city", city, "checkIn", checkIn.toString(), "checkOut", checkOut.toString());
        if (!authClient.configured()) {
            String message = "AMADEUS_CLIENT_ID 或 AMADEUS_CLIENT_SECRET 未配置，无法查询实时航班/酒店价格。";
            logService.log("amadeus.hotels", params.toString(), false, message);
            return ApiResult.unavailable(SOURCE, message);
        }
        String cityCode = iataCodeBook.codeOf(city);
        if (cityCode == null) {
            String message = "未找到城市对应的真实 IATA 代码，请补充 city_airport_code 基础数据。";
            logService.log("amadeus.hotels", params.toString(), false, message);
            return ApiResult.unavailable(SOURCE, message);
        }
        try {
            String token = authClient.token();
            JsonNode hotels = webClient.get()
                    .uri(uri -> uri.scheme("https").host(authClient.host()).path("/v1/reference-data/locations/hotels/by-city")
                            .queryParam("cityCode", cityCode)
                            .queryParam("radius", 5)
                            .queryParam("radiusUnit", "KM")
                            .build())
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (hotels == null || !hotels.has("data") || hotels.path("data").isEmpty()) {
                logService.log("amadeus.hotels", params.toString(), false, "Amadeus 酒店接口未返回酒店列表");
                return ApiResult.unavailable(SOURCE, "当前酒店接口未返回可用报价，请更换日期或目的地。");
            }
            String hotelIds = hotelIds(hotels);
            JsonNode offers = webClient.get()
                    .uri(uri -> uri.scheme("https").host(authClient.host()).path("/v3/shopping/hotel-offers")
                            .queryParam("hotelIds", hotelIds)
                            .queryParam("checkInDate", checkIn)
                            .queryParam("checkOutDate", checkOut)
                            .queryParam("adults", 1)
                            .queryParam("currency", "CNY")
                            .build())
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            boolean ok = offers != null && offers.has("data");
            logService.log("amadeus.hotels", params.toString(), ok, ok ? null : "Amadeus 酒店报价接口未返回 data");
            return ok ? ApiResult.real(SOURCE, offers) : ApiResult.unavailable(SOURCE, "当前酒店接口未返回可用报价，请更换日期或目的地。");
        } catch (Exception ex) {
            logService.log("amadeus.hotels", params.toString(), false, ex.getMessage());
            return ApiResult.unavailable(SOURCE, friendlyError(ex.getMessage()), Map.of("debugError", String.valueOf(ex.getMessage())));
        }
    }

    private String friendlyError(String detail) {
        if (detail != null && detail.contains("401")) {
            return "航班 / 酒店实时价格接口认证失败，请检查 Amadeus Client ID、Client Secret 或 AMADEUS_ENV。当前计划不会使用虚假价格。";
        }
        return "酒店实时价格接口暂时不可用，当前计划不会使用虚假价格。";
    }

    private String hotelIds(JsonNode hotels) {
        StringBuilder ids = new StringBuilder();
        int count = Math.min(10, hotels.path("data").size());
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                ids.append(",");
            }
            ids.append(hotels.path("data").get(i).path("hotelId").asText());
        }
        return ids.toString();
    }
}
