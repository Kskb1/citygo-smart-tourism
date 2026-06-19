package com.citygo.tourism.provider;

import com.citygo.tourism.dto.ApiResult;
import com.citygo.tourism.service.ApiLogService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AmadeusFlightClient {
    private static final String SOURCE = "Amadeus Flight Offers Search";
    private final AmadeusAuthClient authClient;
    private final IataCodeBook iataCodeBook;
    private final WebClient webClient;
    private final ApiLogService logService;

    public AmadeusFlightClient(AmadeusAuthClient authClient, IataCodeBook iataCodeBook, WebClient webClient, ApiLogService logService) {
        this.authClient = authClient;
        this.iataCodeBook = iataCodeBook;
        this.webClient = webClient;
        this.logService = logService;
    }

    public ApiResult search(String fromCity, String toCity, LocalDate date) {
        Map<String, String> params = Map.of("fromCity", fromCity, "toCity", toCity, "date", date.toString());
        if (!authClient.configured()) {
            String message = "AMADEUS_CLIENT_ID 或 AMADEUS_CLIENT_SECRET 未配置，无法查询实时航班/酒店价格。";
            logService.log("amadeus.flights", params.toString(), false, message);
            return ApiResult.unavailable(SOURCE, message);
        }
        String origin = iataCodeBook.codeOf(fromCity);
        String destination = iataCodeBook.codeOf(toCity);
        if (origin == null || destination == null) {
            String message = "未找到城市对应的真实 IATA 代码，请补充 city_airport_code 基础数据。";
            logService.log("amadeus.flights", params.toString(), false, message);
            return ApiResult.unavailable(SOURCE, message);
        }
        try {
            String token = authClient.token();
            JsonNode json = webClient.get()
                    .uri(uri -> uri.scheme("https").host(authClient.host()).path("/v2/shopping/flight-offers")
                            .queryParam("originLocationCode", origin)
                            .queryParam("destinationLocationCode", destination)
                            .queryParam("departureDate", date)
                            .queryParam("adults", 1)
                            .queryParam("currencyCode", "CNY")
                            .queryParam("max", 10)
                            .build())
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            boolean ok = json != null && json.has("data");
            logService.log("amadeus.flights", params.toString(), ok, ok ? null : "Amadeus 航班接口未返回 data");
            return ok ? ApiResult.real(SOURCE, json) : ApiResult.unavailable(SOURCE, "Amadeus 航班接口未返回可用报价。");
        } catch (Exception ex) {
            logService.log("amadeus.flights", params.toString(), false, ex.getMessage());
            return ApiResult.unavailable(SOURCE, friendlyError(ex.getMessage()), Map.of("debugError", String.valueOf(ex.getMessage())));
        }
    }

    private String friendlyError(String detail) {
        if (detail != null && detail.contains("401")) {
            return "航班 / 酒店实时价格接口认证失败，请检查 Amadeus Client ID、Client Secret 或 AMADEUS_ENV。当前计划不会使用虚假价格。";
        }
        return "航班实时价格接口暂时不可用，当前计划不会使用虚假价格。";
    }
}
