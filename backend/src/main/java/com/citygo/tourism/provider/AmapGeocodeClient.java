package com.citygo.tourism.provider;

import com.citygo.tourism.dto.ApiResult;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AmapGeocodeClient {
    private final AmapApiClient client;

    public AmapGeocodeClient(AmapApiClient client) {
        this.client = client;
    }

    public ApiResult geocode(String address, String city) {
        return client.call("amap.geocode", Map.of("address", address, "city", city),
                uri -> uri.scheme("https").host("restapi.amap.com").path("/v3/geocode/geo")
                        .queryParam("key", "{key}")
                        .queryParam("address", address)
                        .queryParam("city", city)
                        .build(client.apiKey()));
    }
}
