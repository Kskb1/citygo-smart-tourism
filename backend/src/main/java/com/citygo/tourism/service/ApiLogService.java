package com.citygo.tourism.service;

import com.citygo.tourism.dto.ApiCallLogEntry;
import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApiLogService {
    private final LinkedList<ApiCallLogEntry> entries = new LinkedList<>();

    public void log(String apiName, String requestParams, boolean success, String errorMessage) {
        synchronized (entries) {
            entries.addFirst(new ApiCallLogEntry(
                    apiName,
                    requestParams,
                    success ? "SUCCESS" : "FAILED",
                    errorMessage,
                    OffsetDateTime.now()));
            if (entries.size() > 500) {
                entries.removeLast();
            }
        }
    }

    public List<ApiCallLogEntry> latest() {
        synchronized (entries) {
            return List.copyOf(entries);
        }
    }
}
