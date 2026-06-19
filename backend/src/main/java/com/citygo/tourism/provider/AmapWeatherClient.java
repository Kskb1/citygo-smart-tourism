package com.citygo.tourism.provider;

import com.citygo.tourism.dto.ApiResult;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AmapWeatherClient {
    private final AmapApiClient client;

    public AmapWeatherClient(AmapApiClient client) {
        this.client = client;
    }

    public ApiResult weather(String city) {
        return client.call("amap.weather", Map.of("city", city),
                uri -> uri.scheme("https").host("restapi.amap.com").path("/v3/weather/weatherInfo")
                        .queryParam("key", "{key}")
                        .queryParam("city", city)
                        .queryParam("extensions", "all")
                        .build(client.apiKey()));
    }
}
