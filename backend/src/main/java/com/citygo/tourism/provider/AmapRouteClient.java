package com.citygo.tourism.provider;

import com.citygo.tourism.dto.ApiResult;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AmapRouteClient {
    private final AmapApiClient client;

    public AmapRouteClient(AmapApiClient client) {
        this.client = client;
    }

    public ApiResult driving(String origin, String destination) {
        return client.call("amap.route", Map.of("origin", origin, "destination", destination),
                uri -> uri.scheme("https").host("restapi.amap.com").path("/v3/direction/driving")
                        .queryParam("key", "{key}")
                        .queryParam("origin", origin)
                        .queryParam("destination", destination)
                        .queryParam("extensions", "base")
                        .build(client.apiKey()));
    }

    public ApiResult walking(String origin, String destination) {
        return client.call("amap.route.walking", Map.of("origin", origin, "destination", destination),
                uri -> uri.scheme("https").host("restapi.amap.com").path("/v3/direction/walking")
                        .queryParam("key", "{key}")
                        .queryParam("origin", origin)
                        .queryParam("destination", destination)
                        .build(client.apiKey()));
    }

    public ApiResult transit(String origin, String destination, String city) {
        return client.call("amap.route.transit", Map.of("origin", origin, "destination", destination, "city", city),
                uri -> uri.scheme("https").host("restapi.amap.com").path("/v3/direction/transit/integrated")
                        .queryParam("key", "{key}")
                        .queryParam("origin", origin)
                        .queryParam("destination", destination)
                        .queryParam("city", city)
                        .build(client.apiKey()));
    }
}
