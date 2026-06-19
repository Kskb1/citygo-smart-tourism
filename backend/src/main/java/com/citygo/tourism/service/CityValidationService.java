package com.citygo.tourism.service;

import com.citygo.tourism.dto.ApiResult;
import com.citygo.tourism.dto.CityResolveResult;
import com.citygo.tourism.exception.CityValidationException;
import com.citygo.tourism.provider.AmapProvider;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CityValidationService {
    private static final String SOURCE = "Amap Open Platform";
    private static final String DIRECT_MUNICIPALITY_SUFFIX = "市";
    private static final int MAX_INPUT_LENGTH = 30;

    private final AmapProvider amapProvider;

    public CityValidationService(AmapProvider amapProvider) {
        this.amapProvider = amapProvider;
    }

    public CityResolveResult resolveOrThrow(String input, String field) {
        CityResolveResult result = resolve(input);
        if (result.valid()) {
            return result;
        }
        throw toException(result, field);
    }

    public CityResolveResult resolve(String input) {
        String original = input == null ? "" : input;
        String trimmed = original.trim();
        String syntaxError = syntaxError(trimmed);
        if (syntaxError != null) {
            return CityResolveResult.invalid(original, syntaxError, List.of(), SOURCE);
        }

        ApiResult geocode = amapProvider.geocode(trimmed, "");
        if (!geocode.realData() || geocode.rawJson() == null) {
            String reason = geocode.errorMessage() == null || geocode.errorMessage().isBlank()
                    ? "城市校验服务暂时不可用，请稍后重试。"
                    : geocode.errorMessage();
            return CityResolveResult.invalid(original, "CITY_VALIDATION_UNAVAILABLE:" + reason, List.of(), SOURCE);
        }

        JsonNode geocodes = geocode.rawJson().path("geocodes");
        if (!geocodes.isArray() || geocodes.isEmpty()) {
            return CityResolveResult.invalid(original, "CITY_NOT_FOUND", List.of(), SOURCE);
        }

        Map<String, CityResolveResult> exactMatches = new LinkedHashMap<>();
        List<String> suggestions = new ArrayList<>();
        for (JsonNode item : geocodes) {
            CityResolveResult candidate = parseCandidate(original, trimmed, item);
            String suggestion = suggestionOf(item);
            if (suggestion != null && !suggestions.contains(suggestion)) {
                suggestions.add(suggestion);
            }
            if (candidate != null) {
                exactMatches.put(candidate.normalizedCityName() + "|" + candidate.adCode(), candidate);
            }
        }

        if (exactMatches.size() == 1) {
            return exactMatches.values().iterator().next().validOnly();
        }
        if (exactMatches.size() > 1) {
            return CityResolveResult.invalid(original, "AMBIGUOUS_CITY", suggestions, SOURCE);
        }
        return CityResolveResult.invalid(original, "CITY_NOT_FOUND", suggestions, SOURCE);
    }

    private CityResolveResult parseCandidate(String original, String trimmed, JsonNode item) {
        String level = text(item, "level");
        String province = normalizeArrayText(text(item, "province"));
        String city = normalizeArrayText(text(item, "city"));
        String district = normalizeArrayText(text(item, "district"));
        String adCode = text(item, "adcode");

        String cityName = city;
        if (cityName.isBlank() && isDirectMunicipality(province, level)) {
            cityName = province;
        }
        if (cityName.isBlank() && level.contains("市")) {
            cityName = normalizeArrayText(text(item, "formatted_address"));
        }
        if (cityName.isBlank()) {
            return null;
        }

        String normalizedCity = ensureCitySuffix(cityName);
        String normalizedInput = normalizeCity(trimmed);
        String cityComparable = normalizeCity(normalizedCity);
        if (!normalizedInput.equals(cityComparable)) {
            return null;
        }

        if (!isCityLevel(level, cityName, province, district)) {
            return null;
        }

        return new CityResolveResult(
                original,
                true,
                normalizedCity,
                province,
                null,
                adCode,
                level,
                null,
                List.of(),
                SOURCE);
    }

    private boolean isCityLevel(String level, String cityName, String province, String district) {
        if (isDirectMunicipality(province, level) && ensureCitySuffix(province).equals(ensureCitySuffix(cityName))) {
            return true;
        }
        if (level == null) {
            return false;
        }
        if (level.contains("市")) {
            return true;
        }
        return !cityName.isBlank() && district.isBlank();
    }

    private boolean isDirectMunicipality(String province, String level) {
        return List.of("北京市", "上海市", "天津市", "重庆市").contains(ensureCitySuffix(province))
                && (level == null || !level.contains("区县"));
    }

    private String syntaxError(String value) {
        if (value.isBlank()) {
            return "EMPTY_CITY";
        }
        if (value.length() > MAX_INPUT_LENGTH) {
            return "INVALID_CITY_FORMAT";
        }
        if (value.length() < 2) {
            return "AMBIGUOUS_CITY";
        }
        if (value.matches(".*[0-9０-９].*")) {
            return "INVALID_CITY_FORMAT";
        }
        if (!value.matches("[\\p{IsHan}·\\s]+")) {
            return "INVALID_CITY_FORMAT";
        }
        return null;
    }

    private CityValidationException toException(CityResolveResult result, String field) {
        String reason = result.failureReason() == null ? "CITY_NOT_FOUND" : result.failureReason();
        if (reason.startsWith("CITY_VALIDATION_UNAVAILABLE")) {
            return new CityValidationException("CITY_VALIDATION_UNAVAILABLE",
                    "城市校验服务暂时不可用，无法确认“" + result.originalInput() + "”是否为真实城市，请稍后重试。",
                    field, result.originalInput(), result.suggestions(), org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
        }
        String code = switch (reason) {
            case "EMPTY_CITY", "INVALID_CITY_FORMAT" -> invalidCodeForField(field);
            case "AMBIGUOUS_CITY" -> "AMBIGUOUS_CITY";
            default -> "CITY_NOT_FOUND";
        };
        String label = "origin".equals(field) ? "出发地" : "目的地";
        String message = switch (code) {
            case "AMBIGUOUS_CITY" -> "无法识别或" + label + "不完整，请输入完整城市名称，例如“武汉”或“武汉市”。";
            case "CITY_NOT_FOUND" -> "无法识别" + label + "“" + result.originalInput() + "”，请确认输入真实完整的城市名称。";
            default -> label + "格式无效，请输入真实完整的城市名称，不要包含数字、特殊符号或多个城市。";
        };
        return new CityValidationException(code, message, field, result.originalInput(), result.suggestions());
    }

    private String invalidCodeForField(String field) {
        return "origin".equals(field) ? "INVALID_ORIGIN_CITY" : "INVALID_DESTINATION_CITY";
    }

    private String suggestionOf(JsonNode item) {
        String province = normalizeArrayText(text(item, "province"));
        String city = normalizeArrayText(text(item, "city"));
        if (city.isBlank() && isDirectMunicipality(province, text(item, "level"))) {
            city = province;
        }
        if (city.isBlank()) {
            return null;
        }
        return ensureCitySuffix(city) + (province.isBlank() || ensureCitySuffix(city).equals(ensureCitySuffix(province)) ? "" : "，" + province);
    }

    private String normalizeCity(String value) {
        return ensureCitySuffix(value)
                .replaceAll("(市|地区|自治州|特别行政区)$", "")
                .replaceAll("\\s+", "");
    }

    private String ensureCitySuffix(String value) {
        String clean = normalizeArrayText(value).replaceAll("\\s+", "");
        if (clean.isBlank()) {
            return "";
        }
        if (clean.endsWith("市") || clean.endsWith("地区") || clean.endsWith("自治州") || clean.endsWith("盟")) {
            return clean;
        }
        return clean + DIRECT_MUNICIPALITY_SUFFIX;
    }

    private String normalizeArrayText(String value) {
        if (value == null || value.isBlank() || "[]".equals(value)) {
            return "";
        }
        return value.trim();
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return "";
        }
        return node.path(field).asText("");
    }
}
