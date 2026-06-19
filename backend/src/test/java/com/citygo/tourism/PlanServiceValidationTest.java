package com.citygo.tourism;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.citygo.tourism.dto.ApiResult;
import com.citygo.tourism.dto.PlanGenerateRequest;
import com.citygo.tourism.dto.SpotSearchData;
import com.citygo.tourism.dto.TrainSearchResponse;
import com.citygo.tourism.exception.PlanningValidationException;
import com.citygo.tourism.provider.AmapProvider;
import com.citygo.tourism.provider.TrainProvider;
import com.citygo.tourism.service.PlanService;
import com.citygo.tourism.service.PriceEstimateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanServiceValidationTest {
    @Mock
    private AmapProvider amapProvider;

    @Mock
    private TrainProvider trainProvider;

    @Mock
    private PriceEstimateService priceEstimateService;

    @Test
    void noPlannablePoiStopsBeforeBudgetEstimate() {
        PlanService service = new PlanService(amapProvider, trainProvider, priceEstimateService);
        PlanGenerateRequest request = new PlanGenerateRequest(
                "成都市",
                "武汉市",
                LocalDate.of(2026, 7, 10),
                3,
                BigDecimal.valueOf(3000),
                1,
                List.of("景点"),
                "train",
                "comfort",
                "standard",
                "mixed",
                true,
                1,
                "high-speed-second",
                List.of());

        when(amapProvider.searchPoi("武汉市", "景点"))
                .thenReturn(ApiResult.real("Amap Open Platform", new SpotSearchData("武汉市", "景点", List.of()), null));
        when(amapProvider.weather("武汉市")).thenReturn(ApiResult.unavailable("Amap Open Platform", "weather unavailable"));
        when(amapProvider.geocode("成都市", "成都市")).thenReturn(ApiResult.unavailable("Amap Open Platform", "geocode unavailable"));
        when(amapProvider.geocode("武汉市", "武汉市")).thenReturn(ApiResult.unavailable("Amap Open Platform", "geocode unavailable"));
        when(amapProvider.searchHotels("武汉市", "酒店", null, null, 3000, 1, 8))
                .thenReturn(ApiResult.unavailable("Amap Open Platform", "hotels unavailable"));
        when(amapProvider.searchAirports("武汉市", "机场"))
                .thenReturn(ApiResult.unavailable("Amap Open Platform", "airports unavailable"));
        when(trainProvider.search("成都市", "武汉市", LocalDate.of(2026, 7, 10)))
                .thenReturn(new TrainSearchResponse(false, "official_redirect", "official only", "https://www.12306.cn/"));

        assertThatThrownBy(() -> service.generate(request))
                .isInstanceOf(PlanningValidationException.class)
                .extracting("code")
                .isEqualTo("NO_PLANNABLE_POI");
        verify(priceEstimateService, never()).estimate(any(), any());
    }
}
