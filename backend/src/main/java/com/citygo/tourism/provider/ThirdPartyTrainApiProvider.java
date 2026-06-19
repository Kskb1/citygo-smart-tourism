package com.citygo.tourism.provider;

import com.citygo.tourism.dto.TrainSearchResponse;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class ThirdPartyTrainApiProvider {
    private final Official12306RedirectProvider officialRedirectProvider;

    public ThirdPartyTrainApiProvider(Official12306RedirectProvider officialRedirectProvider) {
        this.officialRedirectProvider = officialRedirectProvider;
    }

    public TrainSearchResponse search(String fromCity, String toCity, LocalDate date) {
        TrainSearchResponse official = officialRedirectProvider.search(fromCity, toCity, date);
        return new TrainSearchResponse(
                false,
                "third_party_reserved",
                "已选择第三方高铁 API 模式，但当前项目尚未接入具体合规供应商 SDK；不会伪造余票或票价。",
                official.officialUrl());
    }
}
