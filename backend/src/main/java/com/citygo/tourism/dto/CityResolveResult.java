package com.citygo.tourism.dto;

import java.util.List;

public record CityResolveResult(
        String originalInput,
        boolean valid,
        String normalizedCityName,
        String provinceName,
        String cityCode,
        String adCode,
        String level,
        String failureReason,
        List<String> suggestions,
        String dataSource
) {
    public static CityResolveResult invalid(String input, String reason, List<String> suggestions, String dataSource) {
        return new CityResolveResult(input, false, null, null, null, null, null, reason,
                suggestions == null ? List.of() : suggestions, dataSource);
    }

    public CityResolveResult validOnly() {
        return new CityResolveResult(originalInput, true, normalizedCityName, provinceName, cityCode, adCode, level,
                null, suggestions == null ? List.of() : suggestions, dataSource);
    }
}
