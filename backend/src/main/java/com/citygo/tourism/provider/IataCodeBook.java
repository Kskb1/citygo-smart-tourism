package com.citygo.tourism.provider;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class IataCodeBook {
    private final Map<String, String> cityCodes = Map.ofEntries(
            Map.entry("北京", "BJS"),
            Map.entry("上海", "SHA"),
            Map.entry("广州", "CAN"),
            Map.entry("深圳", "SZX"),
            Map.entry("成都", "CTU"),
            Map.entry("重庆", "CKG"),
            Map.entry("武汉", "WUH"),
            Map.entry("西安", "SIA"),
            Map.entry("杭州", "HGH"),
            Map.entry("南京", "NKG"),
            Map.entry("厦门", "XMN"),
            Map.entry("青岛", "TAO"),
            Map.entry("昆明", "KMG"),
            Map.entry("长沙", "CSX"),
            Map.entry("郑州", "CGO")
    );

    public String codeOf(String cityName) {
        if (cityName == null) {
            return null;
        }
        String normalized = cityName.replace("市", "").trim();
        return cityCodes.get(normalized);
    }
}
