package com.citygo.tourism.dto;

import java.util.List;

public record SpotSearchData(
        String city,
        String keyword,
        List<SpotCardDTO> spots
) {
}
