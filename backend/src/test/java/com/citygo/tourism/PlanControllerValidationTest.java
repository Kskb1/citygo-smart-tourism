package com.citygo.tourism;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.citygo.tourism.controller.ApiExceptionHandler;
import com.citygo.tourism.controller.PlanController;
import com.citygo.tourism.dto.CityResolveResult;
import com.citygo.tourism.dto.PlanGenerateRequest;
import com.citygo.tourism.exception.CityValidationException;
import com.citygo.tourism.service.CityValidationService;
import com.citygo.tourism.service.PlanService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlanController.class)
@Import(ApiExceptionHandler.class)
class PlanControllerValidationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlanService planService;

    @MockBean
    private CityValidationService cityValidationService;

    @Test
    void invalidDestinationReturns400AndDoesNotCallPlanner() throws Exception {
        when(cityValidationService.resolveOrThrow(eq("成都"), eq("origin")))
                .thenReturn(valid("成都", "成都市", "origin"));
        when(cityValidationService.resolveOrThrow(eq("武"), eq("destination")))
                .thenThrow(new CityValidationException("AMBIGUOUS_CITY",
                        "无法识别或目的地不完整，请输入完整城市名称，例如“武汉”或“武汉市”。",
                        "destination", "武", List.of("武汉市，湖北省")));

        mockMvc.perform(post("/api/plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromCity":"成都",
                                  "toCity":"武",
                                  "departureDate":"2026-07-10",
                                  "days":3,
                                  "people":1,
                                  "budget":3000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AMBIGUOUS_CITY"))
                .andExpect(jsonPath("$.field").value("destination"))
                .andExpect(jsonPath("$.input").value("武"));

        verify(planService, never()).generate(any());
    }

    @Test
    void validCitiesAreNormalizedBeforePlanning() throws Exception {
        when(cityValidationService.resolveOrThrow(eq("成都"), eq("origin")))
                .thenReturn(valid("成都", "成都市", "origin"));
        when(cityValidationService.resolveOrThrow(eq("武汉"), eq("destination")))
                .thenReturn(valid("武汉", "武汉市", "destination"));

        mockMvc.perform(post("/api/plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromCity":"成都",
                                  "toCity":"武汉",
                                  "departureDate":"2026-07-10",
                                  "days":3,
                                  "people":1,
                                  "budget":3000
                                }
                                """))
                .andExpect(status().isOk());

        verify(planService).generate(org.mockito.ArgumentMatchers.argThat(request ->
                "成都市".equals(request.fromCity()) && "武汉市".equals(request.toCity())));
    }

    @Test
    void validateCityEndpointReturnsStructuredResult() throws Exception {
        when(cityValidationService.resolveOrThrow(eq("武汉"), eq("destination")))
                .thenReturn(valid("武汉", "武汉市", "destination"));

        mockMvc.perform(get("/api/plans/validate-city")
                        .param("input", "武汉")
                        .param("field", "destination"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.normalizedCityName").value("武汉市"));
    }

    private CityResolveResult valid(String input, String normalized, String field) {
        return new CityResolveResult(input, true, normalized,
                "origin".equals(field) ? "四川省" : "湖北省",
                null,
                "origin".equals(field) ? "510100" : "420100",
                "市",
                null,
                List.of(),
                "Amap Open Platform");
    }
}
