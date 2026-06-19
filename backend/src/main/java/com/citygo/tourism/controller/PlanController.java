package com.citygo.tourism.controller;

import com.citygo.tourism.dto.PlanGenerateRequest;
import com.citygo.tourism.dto.PlanResponse;
import com.citygo.tourism.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping("/generate")
    public PlanResponse generate(@Valid @RequestBody PlanGenerateRequest request) {
        return planService.generate(request);
    }
}
