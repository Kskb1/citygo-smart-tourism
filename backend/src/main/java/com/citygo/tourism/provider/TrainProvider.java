package com.citygo.tourism.provider;

import com.citygo.tourism.config.TrainProperties;
import com.citygo.tourism.dto.TrainSearchResponse;
import com.citygo.tourism.service.ApiLogService;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class TrainProvider {
    private final TrainProperties properties;
    private final ApiLogService logService;
    private final Official12306RedirectProvider officialRedirectProvider;
    private final ThirdPartyTrainApiProvider thirdPartyTrainApiProvider;

    public TrainProvider(TrainProperties properties, ApiLogService logService,
                         Official12306RedirectProvider officialRedirectProvider,
                         ThirdPartyTrainApiProvider thirdPartyTrainApiProvider) {
        this.properties = properties;
        this.logService = logService;
        this.officialRedirectProvider = officialRedirectProvider;
        this.thirdPartyTrainApiProvider = thirdPartyTrainApiProvider;
    }

    public TrainSearchResponse search(String fromCity, String toCity, LocalDate date) {
        String params = "{fromCity=" + fromCity + ", toCity=" + toCity + ", date=" + date + "}";
        if (!properties.thirdPartyConfigured()) {
            logService.log("train.official12306", params, false, "未配置合规高铁实时查询接口，返回 12306 官方跳转。");
            return officialRedirectProvider.search(fromCity, toCity, date);
        }
        logService.log("train.thirdParty", params, false, "第三方高铁 API 模式已预留，但未接入具体供应商 SDK。");
        return thirdPartyTrainApiProvider.search(fromCity, toCity, date);
    }
}
