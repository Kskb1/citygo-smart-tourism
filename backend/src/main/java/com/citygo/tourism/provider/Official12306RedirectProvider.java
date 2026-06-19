package com.citygo.tourism.provider;

import com.citygo.tourism.dto.TrainSearchResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class Official12306RedirectProvider {
    public TrainSearchResponse search(String fromCity, String toCity, LocalDate date) {
        return new TrainSearchResponse(
                false,
                "official_12306_redirect",
                "当前未配置合规高铁实时查询接口，请前往 12306 官方平台查询。不显示伪造价格。",
                officialUrl(fromCity, toCity, date));
    }

    private String officialUrl(String fromCity, String toCity, LocalDate date) {
        String query = URLEncoder.encode(fromCity + " 到 " + toCity + " " + date, StandardCharsets.UTF_8);
        return "https://www.12306.cn/index/?q=" + query;
    }
}
