package com.citygo.tourism.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

public record ApiResult(
        String provider,
        String message,
        Object data,
        String sourceName,
        OffsetDateTime fetchedAt,
        boolean realData,
        JsonNode rawJson,
        String errorMessage
) {
    public static ApiResult real(String sourceName, JsonNode rawJson) {
        return new ApiResult(providerOf(sourceName), null, rawJson, sourceName, OffsetDateTime.now(), true, rawJson, null);
    }

    public static ApiResult real(String sourceName, Object data, JsonNode rawJson) {
        return new ApiResult(providerOf(sourceName), null, data, sourceName, OffsetDateTime.now(), true, rawJson, null);
    }

    public static ApiResult unavailable(String sourceName, String message) {
        return new ApiResult(providerOf(sourceName), message, null, sourceName, OffsetDateTime.now(), false, null, message);
    }

    public static ApiResult unavailable(String sourceName, String message, Object data) {
        return new ApiResult(providerOf(sourceName), message, data, sourceName, OffsetDateTime.now(), false, null, message);
    }

    private static String providerOf(String sourceName) {
        if (sourceName == null) {
            return "unknown";
        }
        String lower = sourceName.toLowerCase();
        if (lower.contains("amap")) {
            return "amap";
        }
        if (lower.contains("amadeus")) {
            return "amadeus";
        }
        if (lower.contains("12306") || lower.contains("train")) {
            return "train";
        }
        return lower.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }
}
