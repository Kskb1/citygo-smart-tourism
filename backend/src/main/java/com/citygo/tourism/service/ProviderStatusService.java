package com.citygo.tourism.service;

import com.citygo.tourism.config.AmapProperties;
import com.citygo.tourism.config.TrainProperties;
import com.citygo.tourism.dto.ProviderStatus;
import org.springframework.stereotype.Service;

@Service
public class ProviderStatusService {
    private final AmapProperties amap;
    private final TrainProperties train;

    public ProviderStatusService(AmapProperties amap, TrainProperties train) {
        this.amap = amap;
        this.train = train;
    }

    public ProviderStatus status() {
        return new ProviderStatus(
                amap.configured(),
                amap.configured(),
                amap.configured(),
                amap.configured(),
                amap.configured(),
                amap.configured(),
                amap.configured(),
                false,
                false,
                train.provider() == null ? "official_redirect" : train.provider(),
                train.thirdPartyConfigured());
    }
}
