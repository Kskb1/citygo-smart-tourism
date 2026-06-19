package com.citygo.tourism.controller;

import com.citygo.tourism.dto.ApiResult;
import com.citygo.tourism.dto.TrainSearchResponse;
import com.citygo.tourism.provider.AmapProvider;
import com.citygo.tourism.provider.TrainProvider;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DataController {
    private final AmapProvider amapProvider;
    private final TrainProvider trainProvider;

    public DataController(AmapProvider amapProvider, TrainProvider trainProvider) {
        this.amapProvider = amapProvider;
        this.trainProvider = trainProvider;
    }

    @GetMapping("/spots/search")
    public ApiResult spots(@RequestParam String city, @RequestParam(defaultValue = "景点") String keyword) {
        return amapProvider.searchPoi(city, keyword);
    }

    @GetMapping("/weather")
    public ApiResult weather(@RequestParam String city) {
        return amapProvider.weather(city);
    }

    @GetMapping("/routes")
    public ApiResult routes(@RequestParam String origin, @RequestParam String destination,
                            @RequestParam(defaultValue = "driving") String mode,
                            @RequestParam(required = false) String city) {
        return amapProvider.route(origin, destination, mode, city);
    }

    @GetMapping("/flights/search")
    public ApiResult flights(@RequestParam String fromCity, @RequestParam String toCity,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResult.unavailable("Airline official query", "当前系统不直接提供实时航班班次、余票和机票价格，请以前往航空公司或正规预订平台查询为准。",
                Map.of(
                        "sourceName", "航空公司/正规平台查询入口",
                        "dataMode", "OFFICIAL_REDIRECT",
                        "realData", false,
                        "fromCity", fromCity,
                        "toCity", toCity,
                        "date", date.toString(),
                        "entries", List.of("航空公司官网", "航旅纵横", "携程/同程/飞猪等正规预订平台"),
                        "notice", "CityGo 不生成航班号、起降时间、票价或余票。"));
    }

    @GetMapping("/hotels/search")
    public ApiResult hotels(@RequestParam String city,
                            @RequestParam(defaultValue = "酒店") String keyword,
                            @RequestParam(required = false) String reference,
                            @RequestParam(required = false) String center,
                            @RequestParam(required = false) Integer radius,
                            @RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return amapProvider.searchHotels(city, keyword, reference, center, radius, page, pageSize);
    }

    @GetMapping("/airports/search")
    public ApiResult airports(@RequestParam String city,
                              @RequestParam(defaultValue = "机场") String keyword) {
        return amapProvider.searchAirports(city, keyword);
    }

    @GetMapping("/trains/search")
    public TrainSearchResponse trains(@RequestParam String fromCity, @RequestParam String toCity,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return trainProvider.search(fromCity, toCity, date);
    }
}
