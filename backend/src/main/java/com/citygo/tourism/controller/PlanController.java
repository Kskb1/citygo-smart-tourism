package com.citygo.tourism.controller;

import com.citygo.tourism.dto.CityResolveResult;
import com.citygo.tourism.dto.PlanGenerateRequest;
import com.citygo.tourism.dto.PlanResponse;
import com.citygo.tourism.service.CityValidationService;
import com.citygo.tourism.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
    private final PlanService planService;
    private final CityValidationService cityValidationService;

    public PlanController(PlanService planService, CityValidationService cityValidationService) {
        this.planService = planService;
        this.cityValidationService = cityValidationService;
    }

    @PostMapping("/generate")
    public PlanResponse generate(@Valid @RequestBody PlanGenerateRequest request) {
        CityResolveResult origin = cityValidationService.resolveOrThrow(request.fromCity(), "origin");
        CityResolveResult destination = cityValidationService.resolveOrThrow(request.toCity(), "destination");
        return planService.generate(normalizedRequest(request, origin.normalizedCityName(), destination.normalizedCityName()));
    }

    @GetMapping("/validate-city")
    public CityResolveResult validateCity(@RequestParam String input, @RequestParam(defaultValue = "destination") String field) {
        return cityValidationService.resolveOrThrow(input, field);
    }

    private PlanGenerateRequest normalizedRequest(PlanGenerateRequest request, String fromCity, String toCity) {
        return new PlanGenerateRequest(
                fromCity,
                toCity,
                request.departureDate(),
                request.days(),
                request.budget(),
                request.people(),
                request.interests(),
                request.trafficPreference(),
                request.hotelPreference(),
                request.foodPreference(),
                request.localTransportPreference(),
                request.roundTrip(),
                request.roomCount(),
                request.trainSeatType(),
                request.selectedSpots());
    }
}
