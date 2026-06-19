package com.citygo.tourism.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PlanGenerateRequest(
        @NotBlank String fromCity,
        @NotBlank String toCity,
        @JsonAlias("startDate") @NotNull LocalDate departureDate,
        @Min(1) int days,
        BigDecimal budget,
        @JsonAlias("peopleCount") @Min(1) int people,
        @JsonAlias("preferences") List<String> interests,
        @JsonAlias("transportPreference") String trafficPreference,
        String hotelPreference,
        String foodPreference,
        String localTransportPreference,
        Boolean roundTrip,
        Integer roomCount,
        String trainSeatType,
        List<SelectedSpotDTO> selectedSpots
) {
}
