package com.citygo.tourism;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.citygo.tourism.dto.ApiResult;
import com.citygo.tourism.dto.CityResolveResult;
import com.citygo.tourism.exception.CityValidationException;
import com.citygo.tourism.provider.AmapProvider;
import com.citygo.tourism.service.CityValidationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CityValidationServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AmapProvider amapProvider;

    @Test
    void normalizesCityNameFromAmapGeocode() throws Exception {
        when(amapProvider.geocode("成都", "")).thenReturn(geocode("""
                [{"province":"四川省","city":"成都市","district":[],"adcode":"510100","level":"市"}]
                """));
        CityValidationService service = new CityValidationService(amapProvider);

        CityResolveResult result = service.resolveOrThrow("成都", "origin");

        assertThat(result.valid()).isTrue();
        assertThat(result.normalizedCityName()).isEqualTo("成都市");
        assertThat(result.provinceName()).isEqualTo("四川省");
        assertThat(result.adCode()).isEqualTo("510100");
    }

    @ParameterizedTest
    @CsvSource({
            "武汉,湖北省,武汉市,420100",
            "武汉市,湖北省,武汉市,420100",
            "上海,上海市,上海市,310000",
            "重庆,重庆市,重庆市,500000",
            "西安,陕西省,西安市,610100",
            "西安市,陕西省,西安市,610100"
    })
    void acceptsCommonValidCityInputs(String input, String province, String city, String adcode) throws Exception {
        String cityField = province.equals(city) ? "[]" : "\"" + city + "\"";
        when(amapProvider.geocode(input, "")).thenReturn(geocode("""
                [{"province":"%s","city":%s,"district":[],"adcode":"%s","level":"市"}]
                """.formatted(province, cityField, adcode)));
        CityValidationService service = new CityValidationService(amapProvider);

        CityResolveResult result = service.resolveOrThrow(input, "destination");

        assertThat(result.normalizedCityName()).isEqualTo(city);
        assertThat(result.adCode()).isEqualTo(adcode);
    }

    @Test
    void normalizesDirectMunicipality() throws Exception {
        when(amapProvider.geocode("北京", "")).thenReturn(geocode("""
                [{"province":"北京市","city":[],"district":[],"adcode":"110000","level":"市"}]
                """));
        CityValidationService service = new CityValidationService(amapProvider);

        CityResolveResult result = service.resolveOrThrow("北京", "destination");

        assertThat(result.normalizedCityName()).isEqualTo("北京市");
    }

    @Test
    void rejectsSingleCharacterPrefixWithoutCallingAmap() {
        CityValidationService service = new CityValidationService(amapProvider);

        assertThatThrownBy(() -> service.resolveOrThrow("武", "destination"))
                .isInstanceOf(CityValidationException.class)
                .extracting("code")
                .isEqualTo("AMBIGUOUS_CITY");
    }

    @Test
    void rejectsInvalidCharactersAndDigits() {
        CityValidationService service = new CityValidationService(amapProvider);

        assertThatThrownBy(() -> service.resolveOrThrow("成都123", "destination"))
                .isInstanceOf(CityValidationException.class)
                .extracting("code")
                .isEqualTo("INVALID_DESTINATION_CITY");
        assertThatThrownBy(() -> service.resolveOrThrow("abc", "origin"))
                .isInstanceOf(CityValidationException.class)
                .extracting("code")
                .isEqualTo("INVALID_ORIGIN_CITY");
    }

    @Test
    void rejectsCityNotFound() throws Exception {
        when(amapProvider.geocode("不存在市", "")).thenReturn(geocode("[]"));
        CityValidationService service = new CityValidationService(amapProvider);

        assertThatThrownBy(() -> service.resolveOrThrow("不存在市", "destination"))
                .isInstanceOf(CityValidationException.class)
                .extracting("code")
                .isEqualTo("CITY_NOT_FOUND");
    }

    @Test
    void rejectsAmbiguousExactCandidates() throws Exception {
        when(amapProvider.geocode("重庆", "")).thenReturn(geocode("""
                [
                  {"province":"重庆市","city":[],"district":[],"adcode":"500000","level":"市"},
                  {"province":"重庆省","city":"重庆市","district":[],"adcode":"990100","level":"市"}
                ]
                """));
        CityValidationService service = new CityValidationService(amapProvider);

        assertThatThrownBy(() -> service.resolveOrThrow("重庆", "destination"))
                .isInstanceOf(CityValidationException.class)
                .extracting("code")
                .isEqualTo("AMBIGUOUS_CITY");
    }

    @Test
    void rejectsExternalValidationUnavailable() {
        when(amapProvider.geocode("武汉", "")).thenReturn(ApiResult.unavailable("Amap Open Platform", "timeout"));
        CityValidationService service = new CityValidationService(amapProvider);

        assertThatThrownBy(() -> service.resolveOrThrow("武汉", "destination"))
                .isInstanceOf(CityValidationException.class)
                .extracting("code")
                .isEqualTo("CITY_VALIDATION_UNAVAILABLE");
    }

    private ApiResult geocode(String geocodesJson) throws Exception {
        JsonNode raw = objectMapper.readTree("""
                {"status":"1","geocodes":%s}
                """.formatted(geocodesJson));
        return ApiResult.real("Amap Open Platform", raw);
    }
}
