package com.citygo.tourism.provider;

import com.citygo.tourism.dto.ApiResult;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AmapPoiClient {
    private final AmapApiClient client;

    public AmapPoiClient(AmapApiClient client) {
        this.client = client;
    }

    public ApiResult search(String city, String keyword) {
        return search(city, keyword, 1, 20);
    }

    public ApiResult search(String city, String keyword, int page, int pageSize) {
        return client.call("amap.poi", Map.of("city", city, "keywords", keyword),
                uri -> uri.scheme("https").host("restapi.amap.com").path("/v3/place/text")
                        .queryParam("key", "{key}")
                        .queryParam("city", city)
                        .queryParam("keywords", keyword)
                        .queryParam("extensions", "all")
                        .queryParam("page", Math.max(1, page))
                        .queryParam("offset", Math.min(Math.max(1, pageSize), 25))
                        .build(client.apiKey()));
    }

    public ApiResult around(String location, String keyword, int radius, int page, int pageSize) {
        return client.call("amap.poi.around", Map.of("location", location, "keywords", keyword),
                uri -> uri.scheme("https").host("restapi.amap.com").path("/v3/place/around")
                        .queryParam("key", "{key}")
                        .queryParam("location", location)
                        .queryParam("keywords", keyword)
                        .queryParam("radius", Math.max(100, radius))
                        .queryParam("extensions", "all")
                        .queryParam("page", Math.max(1, page))
                        .queryParam("offset", Math.min(Math.max(1, pageSize), 25))
                        .build(client.apiKey()));
    }
}
