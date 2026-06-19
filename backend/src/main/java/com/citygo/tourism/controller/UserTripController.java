package com.citygo.tourism.controller;

import com.citygo.tourism.dto.SaveTripRequest;
import com.citygo.tourism.dto.UpdateTripTitleRequest;
import com.citygo.tourism.dto.UserTripDTO;
import com.citygo.tourism.service.UserTripService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
public class UserTripController {
    private final UserTripService tripService;

    public UserTripController(UserTripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public Map<String, Object> save(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody SaveTripRequest request) {
        return tripService.save(authorization, request);
    }

    @GetMapping
    public List<UserTripDTO> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fromCity,
            @RequestParam(required = false) String toCity,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tripService.listMine(authorization, keyword, fromCity, toCity, page, size);
    }

    @GetMapping("/{id}")
    public UserTripDTO detail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable long id) {
        return tripService.detail(authorization, id);
    }

    @PutMapping("/{id}")
    public UserTripDTO updateTitle(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable long id,
            @Valid @RequestBody UpdateTripTitleRequest request) {
        return tripService.updateTitle(authorization, id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable long id) {
        tripService.delete(authorization, id);
        return Map.of("message", "行程已删除");
    }
}
