package com.citygo.tourism.provider;

import com.citygo.tourism.config.AmadeusProperties;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AmadeusAuthClient {
    private final AmadeusProperties properties;
    private final WebClient webClient;

    public AmadeusAuthClient(AmadeusProperties properties, WebClient webClient) {
        this.properties = properties;
        this.webClient = webClient;
    }

    public boolean configured() {
        return properties.configured();
    }

    public String token() {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        Map<?, ?> response = webClient.post()
                .uri(properties.baseUrl() + "/v1/security/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("Amadeus 未返回 access_token");
        }
        return response.get("access_token").toString();
    }

    public String baseUrl() {
        return properties.baseUrl();
    }

    public String host() {
        return properties.baseUrl().replace("https://", "");
    }
}
