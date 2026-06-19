package com.citygo.tourism.controller;

import com.citygo.tourism.dto.ProviderStatus;
import com.citygo.tourism.service.ProviderStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final ProviderStatusService statusService;

    public SystemController(ProviderStatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/provider-status")
    public ProviderStatus status() {
        return statusService.status();
    }
}
