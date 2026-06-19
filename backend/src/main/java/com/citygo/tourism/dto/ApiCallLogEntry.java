package com.citygo.tourism.dto;

import java.time.OffsetDateTime;

public record ApiCallLogEntry(
        String apiName,
        String requestParams,
        String status,
        String errorMessage,
        OffsetDateTime createdAt
) {
}
