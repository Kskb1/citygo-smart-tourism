package com.citygo.tourism.service;

import com.citygo.tourism.dto.ApiResult;
import com.citygo.tourism.provider.AmadeusHotelClient;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class HotelPriceService {
    private final AmadeusHotelClient hotelClient;

    public HotelPriceService(AmadeusHotelClient hotelClient) {
        this.hotelClient = hotelClient;
    }

    public ApiResult search(String city, LocalDate checkIn, LocalDate checkOut) {
        return hotelClient.search(city, checkIn, checkOut);
    }
}
