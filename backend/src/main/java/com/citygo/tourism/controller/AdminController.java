package com.citygo.tourism.controller;

import com.citygo.tourism.config.PriceEstimateProperties;
import com.citygo.tourism.dto.ApiCallLogEntry;
import com.citygo.tourism.dto.ProviderStatus;
import com.citygo.tourism.dto.UserTripDTO;
import com.citygo.tourism.service.ApiLogService;
import com.citygo.tourism.service.AuthService;
import com.citygo.tourism.service.ProviderStatusService;
import com.citygo.tourism.service.UserTripService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ProviderStatusService statusService;
    private final ApiLogService logService;
    private final AuthService authService;
    private final UserTripService tripService;
    private final PriceEstimateProperties estimateProperties;

    public AdminController(
            ProviderStatusService statusService,
            ApiLogService logService,
            AuthService authService,
            UserTripService tripService,
            PriceEstimateProperties estimateProperties) {
        this.statusService = statusService;
        this.logService = logService;
        this.authService = authService;
        this.tripService = tripService;
        this.estimateProperties = estimateProperties;
    }

    @GetMapping("/provider-status")
    public ProviderStatus providerStatus(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.requireAdmin(authorization);
        return statusService.status();
    }

    @GetMapping("/api-logs")
    public List<ApiCallLogEntry> apiLogs(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.requireAdmin(authorization);
        return logService.latest();
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.requireAdmin(authorization);
        List<ApiCallLogEntry> logs = logService.latest();
        long success = logs.stream().filter(log -> "SUCCESS".equals(log.status())).count();
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("userCount", authService.users().size());
        dashboard.put("normalUserCount", authService.countByRole("USER"));
        dashboard.put("adminCount", authService.countByRole("ADMIN"));
        dashboard.put("tripCount", tripService.activeCount());
        dashboard.put("todayTripCount", tripService.todayCount());
        dashboard.put("selectedSpotCount", 0);
        dashboard.put("todayApiCalls", logs.size());
        dashboard.put("apiSuccessRate", logs.isEmpty() ? 0 : (success * 100.0 / logs.size()));
        dashboard.put("recentUsers", authService.users());
        dashboard.put("recentTrips", tripService.recent(8));
        dashboard.put("notice", "后台展示运行期用户、API 调用日志和已保存行程统计。");
        return dashboard;
    }

    @GetMapping("/price-estimate-rules")
    public Map<String, Object> priceEstimateRules(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.requireAdmin(authorization);
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("priceMode", "RULE_ESTIMATED");
        rules.put("ruleVersion", estimateProperties.version());
        rules.put("sourceName", "CityGo预算规则引擎");
        rules.put("notice", "该规则只生成非实时预算参考价，不代表实时市场报价、官方报价、库存或可预订结果。");
        rules.put("factors", Map.of(
                "weekendFactor", estimateProperties.weekendFactor(),
                "shortNoticeFactor", estimateProperties.shortNoticeFactor(),
                "normalNoticeFactor", estimateProperties.normalNoticeFactor(),
                "emergencyReserveRate", estimateProperties.emergencyReserveRate()
        ));
        rules.put("flight", estimateProperties.flight());
        rules.put("train", estimateProperties.train());
        rules.put("hotel", estimateProperties.hotel());
        rules.put("food", estimateProperties.food());
        rules.put("localTransport", estimateProperties.localTransport());
        return rules;
    }

    @GetMapping("/trips")
    public List<UserTripDTO> trips(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fromCity,
            @RequestParam(required = false) String toCity,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return tripService.listAllForAdmin(authorization, keyword, fromCity, toCity, page, size);
    }

    @GetMapping("/trips/{id}")
    public UserTripDTO tripDetail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable long id) {
        return tripService.adminDetail(authorization, id);
    }

    @GetMapping({"/cache/spots", "/cache/hotels", "/cache/flights", "/cache/weather", "/cache/routes"})
    public List<Object> emptyCacheView(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.requireAdmin(authorization);
        return List.of();
    }
}
