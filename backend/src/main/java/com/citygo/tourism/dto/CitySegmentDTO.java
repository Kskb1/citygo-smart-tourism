package com.citygo.tourism.dto;

import java.util.List;

public record CitySegmentDTO(
        String cityName,
        String segmentType,
        List<SpotCardDTO> spots,
        String travelNotice,
        int recommendedStayDays,
        boolean feasibleInCurrentPlan
) {
}
