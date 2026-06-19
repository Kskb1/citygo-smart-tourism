package com.citygo.tourism.service;

import com.citygo.tourism.dto.ApiResult;
import com.citygo.tourism.dto.BudgetReferenceDTO;
import com.citygo.tourism.dto.BudgetSummaryDTO;
import com.citygo.tourism.dto.CitySegmentDTO;
import com.citygo.tourism.dto.DailyPlanDTO;
import com.citygo.tourism.dto.DataSourceDTO;
import com.citygo.tourism.dto.FeasibilityDTO;
import com.citygo.tourism.dto.PlanActivityDTO;
import com.citygo.tourism.dto.PlanGenerateRequest;
import com.citygo.tourism.dto.PlanResponse;
import com.citygo.tourism.dto.RouteDTO;
import com.citygo.tourism.dto.SelectedSpotDTO;
import com.citygo.tourism.dto.SpotCardDTO;
import com.citygo.tourism.dto.SpotSearchData;
import com.citygo.tourism.dto.TrainSearchResponse;
import com.citygo.tourism.dto.TransportPlanDTO;
import com.citygo.tourism.dto.WeatherDTO;
import com.citygo.tourism.exception.PlanningValidationException;
import com.citygo.tourism.provider.AmapProvider;
import com.citygo.tourism.provider.TrainProvider;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PlanService {
    private final AmapProvider amapProvider;
    private final TrainProvider trainProvider;
    private final PriceEstimateService priceEstimateService;

    public PlanService(AmapProvider amapProvider, TrainProvider trainProvider, PriceEstimateService priceEstimateService) {
        this.amapProvider = amapProvider;
        this.trainProvider = trainProvider;
        this.priceEstimateService = priceEstimateService;
    }

    public PlanResponse generate(PlanGenerateRequest request) {
        Map<String, ApiResult> data = new LinkedHashMap<>();
        String keyword = request.interests() == null || request.interests().isEmpty()
                ? "景点"
                : String.join("|", request.interests());

        data.put("spots", amapProvider.searchPoi(request.toCity(), keyword));
        data.put("weather", amapProvider.weather(request.toCity()));
        data.put("route", routeBetweenCities(request.fromCity(), request.toCity()));
        data.put("hotels", amapProvider.searchHotels(request.toCity(), "酒店", null, null, 3000, 1, 8));
        data.put("airports", amapProvider.searchAirports(request.toCity(), "机场"));
        TrainSearchResponse train = trainProvider.search(request.fromCity(), request.toCity(), request.departureDate());

        PlanResponse.BudgetEstimate budget = estimateBudget(request, data);
        SpotSearchData spotSearch = dataObject(data.get("spots"), SpotSearchData.class);
        List<SpotCardDTO> autoSpots = spotSearch == null ? List.of() : spotSearch.spots();
        List<SpotCardDTO> selectedSpots = selectedSpotCards(request);
        List<SpotCardDTO> destinationSelectedSpots = destinationSpots(request, selectedSpots);
        List<SpotCardDTO> crossCitySpots = crossCitySpots(request, selectedSpots);
        List<SpotCardDTO> planSpots = mergeSpots(destinationSelectedSpots, autoSpots);
        if (planSpots.isEmpty()) {
            throw new PlanningValidationException("NO_PLANNABLE_POI",
                    "已识别该城市，但暂未获取到可用于规划的真实景点数据。请稍后重试，或先在目的地探索页加入真实景点。");
        }
        WeatherDTO weather = dataObject(data.get("weather"), WeatherDTO.class);
        RouteDTO route = dataObject(data.get("route"), RouteDTO.class);
        TransportPlanDTO outbound = outboundTransport(request, train);
        TransportPlanDTO inbound = returnTransport(request, train);
        BudgetReferenceDTO budgetReference = priceEstimateService.estimate(request, route);
        BudgetSummaryDTO budgetSummary = budgetSummary(request, budget, budgetReference);
        FeasibilityDTO feasibility = evaluateFeasibility(request, selectedSpots, budgetSummary, data);
        List<CitySegmentDTO> citySegments = citySegments(request, destinationSelectedSpots, crossCitySpots);
        List<String> warnings = dataWarnings(data, train, selectedSpots, request);
        List<DailyPlanDTO> dailyPlans = dailyPlans(request, planSpots);
        List<DataSourceDTO> sources = dataSources(data, train);

        String title = request.fromCity() + "出发 · " + request.toCity() + " " + request.days() + "日智慧旅游计划";
        String summary = summary(request, data, selectedSpots);
        String budgetConclusion = budgetConclusion(request, budgetSummary);

        return new PlanResponse(
                title,
                summary,
                request.fromCity(),
                request.toCity(),
                request.departureDate().toString(),
                request.days(),
                Math.max(1, request.people()),
                request.budget(),
                feasibility.recommendedDays(),
                budgetConclusion,
                feasibility,
                citySegments,
                outbound,
                inbound,
                weather,
                dailyPlans,
                planSpots,
                route,
                budgetSummary,
                warnings,
                sources,
                data,
                request,
                warnings,
                data,
                train,
                budget);
    }

    private List<String> dataWarnings(Map<String, ApiResult> data, TrainSearchResponse train, List<SpotCardDTO> selectedSpots, PlanGenerateRequest request) {
        List<String> warnings = new ArrayList<>();
        data.forEach((name, result) -> {
            if (!result.realData()) {
                warnings.add(name + " 未获取到真实数据：" + result.errorMessage());
            }
        });
        if (!train.realData()) {
            warnings.add(train.message());
        }
        for (SpotCardDTO spot : selectedSpots) {
            if (spot.cityName() != null && !sameCity(spot.cityName(), request.toCity())) {
                warnings.add("你选择的景点「" + spot.name() + "」位于 " + spot.cityName()
                        + "，不在主目的地 " + request.toCity() + "，系统会将其作为跨城市扩展景点。");
            }
        }
        warnings.add("门票价格请以景区官方公告为准，本系统不会编造门票价格。");
        return warnings;
    }

    private PlanResponse.BudgetEstimate estimateBudget(PlanGenerateRequest request, Map<String, ApiResult> data) {
        int people = Math.max(1, request.people());
        int days = Math.max(1, request.days());
        BigDecimal food = BigDecimal.valueOf(120L * days * people);
        BigDecimal localTraffic = BigDecimal.valueOf(30L * days * people);
        BigDecimal flightCost = null;
        BigDecimal hotelCost = null;
        List<String> notes = new ArrayList<>();
        notes.add("餐饮费用为规则估算值，每人每天 120 元，不是实时价格。");
        notes.add("市内交通为规则估算值，每人每天 30 元，不是实时价格。");
        notes.add("住宿费用按用户预算规则估算，不代表酒店实时房价或房态。");
        notes.add("当前不接入实时航班班次、余票和机票价格；跨城市交通请以 12306、航司或正规平台为准。");
        BigDecimal total = food.add(localTraffic)
                .add(flightCost == null ? BigDecimal.ZERO : flightCost)
                .add(hotelCost == null ? BigDecimal.ZERO : hotelCost);
        return new PlanResponse.BudgetEstimate(flightCost, hotelCost, food, localTraffic, total, notes);
    }

    private ApiResult routeBetweenCities(String fromCity, String toCity) {
        ApiResult from = amapProvider.geocode(fromCity, fromCity);
        ApiResult to = amapProvider.geocode(toCity, toCity);
        String origin = firstLocation(from);
        String destination = firstLocation(to);
        if (origin == null || destination == null) {
            String reason = !from.realData() ? from.errorMessage()
                    : !to.realData() ? to.errorMessage()
                    : "高德地理编码未返回可用经纬度";
            return ApiResult.unavailable("Amap Open Platform", "城市经纬度解析失败，无法获取真实路线数据：" + reason);
        }
        return amapProvider.routeWithNames(origin, destination, fromCity, toCity);
    }

    private String firstLocation(ApiResult result) {
        if (result == null || !result.realData() || result.rawJson() == null) {
            return null;
        }
        JsonNode geocodes = result.rawJson().path("geocodes");
        if (!geocodes.isArray() || geocodes.isEmpty()) {
            return null;
        }
        String location = geocodes.get(0).path("location").asText(null);
        return location == null || location.isBlank() ? null : location;
    }

    private <T> T dataObject(ApiResult result, Class<T> type) {
        if (result == null || result.data() == null || !type.isInstance(result.data())) {
            return null;
        }
        return type.cast(result.data());
    }

    private TransportPlanDTO outboundTransport(PlanGenerateRequest request, TrainSearchResponse train) {
        return new TransportPlanDTO(
                request.fromCity() + " → " + request.toCity(),
                "airport_official_query",
                "当前为跨城市行程时，建议根据实际航班或铁路班次预留半天至一天交通时间；具体班次、余票和票价请以官方平台为准。",
                train.officialUrl(),
                "高德 POI 可用于机场位置和地面接驳路线；航班请前往航空公司或正规平台查询，高铁请前往 12306 官方渠道。",
                false,
                null);
    }

    private TransportPlanDTO returnTransport(PlanGenerateRequest request, TrainSearchResponse train) {
        return new TransportPlanDTO(
                request.toCity() + " → " + request.fromCity(),
                "official_query_required",
                "返程需要以 12306、航司或合规售票平台的实时结果为准；当前计划不编造返程票价。",
                train.officialUrl(),
                "请使用返程日期在 12306 官方平台查询。",
                false,
                null);
    }

    private BudgetSummaryDTO budgetSummary(PlanGenerateRequest request, PlanResponse.BudgetEstimate budget, BudgetReferenceDTO budgetReference) {
        BigDecimal userBudget = request.budget() == null ? BigDecimal.ZERO : request.budget();
        BigDecimal knownRealCost = BigDecimal.ZERO;
        if (budget.flightOrTrainCost() != null) {
            knownRealCost = knownRealCost.add(budget.flightOrTrainCost());
        }
        if (budget.hotelCost() != null) {
            knownRealCost = knownRealCost.add(budget.hotelCost());
        }
        BigDecimal hotelEstimate = budget.hotelCost() == null
                ? BigDecimal.valueOf(300L * Math.max(0, request.days() - 1))
                : BigDecimal.ZERO;
        BigDecimal estimatedCost = budget.foodEstimate().add(budget.localTrafficEstimate()).add(hotelEstimate);
        BigDecimal totalMinCost = knownRealCost.add(estimatedCost);
        BigDecimal remaining = userBudget.subtract(totalMinCost);
        List<String> warnings = new ArrayList<>(budget.notes());
        warnings.add("住宿建议预算为规则估算，按每晚约 200-400 元预留，最终以预订页面为准。");
        warnings.add("门票以景区官方公告为准，不纳入固定估算。");
        return new BudgetSummaryDTO(
                userBudget,
                knownRealCost,
                estimatedCost,
                totalMinCost,
                remaining,
                budget.foodEstimate(),
                budget.localTrafficEstimate(),
                hotelEstimate,
                "门票以景区官方公告为准",
                budget.flightOrTrainCost(),
                "未接入实时航班/机票价格；请以 12306、航司或正规平台为准",
                budgetReference,
                warnings);
    }

    private List<SpotCardDTO> selectedSpotCards(PlanGenerateRequest request) {
        if (request.selectedSpots() == null || request.selectedSpots().isEmpty()) {
            return List.of();
        }
        List<SpotCardDTO> spots = new ArrayList<>();
        int index = 0;
        for (SelectedSpotDTO spot : request.selectedSpots()) {
            spots.add(new SpotCardDTO(
                    "selected-" + (++index) + "-" + nullToEmpty(spot.name()),
                    spot.name(),
                    spot.address(),
                    spot.type(),
                    spot.cityName(),
                    locationText(spot),
                    spot.longitude(),
                    spot.latitude(),
                    null,
                    null,
                    blankOrDefault(spot.recommendationReason(), "用户从目的地探索页加入的候选景点。"),
                    "user_selection",
                    null,
                    true));
        }
        return spots;
    }

    private String locationText(SelectedSpotDTO spot) {
        if (spot.longitude() == null || spot.latitude() == null) {
            return null;
        }
        return spot.longitude() + "," + spot.latitude();
    }

    private List<SpotCardDTO> mergeSpots(List<SpotCardDTO> selected, List<SpotCardDTO> autoSpots) {
        List<SpotCardDTO> merged = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (SpotCardDTO spot : selected) {
            if (seen.add(spotKey(spot))) {
                merged.add(spot);
            }
        }
        for (SpotCardDTO spot : autoSpots) {
            if (seen.add(spotKey(spot))) {
                merged.add(spot);
            }
        }
        return merged;
    }

    private String spotKey(SpotCardDTO spot) {
        return nullToEmpty(spot.name()) + "|" + nullToEmpty(spot.cityName()) + "|" + nullToEmpty(spot.address());
    }

    private List<SpotCardDTO> destinationSpots(PlanGenerateRequest request, List<SpotCardDTO> selectedSpots) {
        return selectedSpots.stream()
                .filter(spot -> spot.cityName() == null || sameCity(spot.cityName(), request.toCity()))
                .toList();
    }

    private List<SpotCardDTO> crossCitySpots(PlanGenerateRequest request, List<SpotCardDTO> selectedSpots) {
        return selectedSpots.stream()
                .filter(spot -> spot.cityName() != null && !sameCity(spot.cityName(), request.toCity()))
                .toList();
    }

    private List<CitySegmentDTO> citySegments(PlanGenerateRequest request, List<SpotCardDTO> destinationSpots, List<SpotCardDTO> crossCitySpots) {
        List<CitySegmentDTO> segments = new ArrayList<>();
        segments.add(new CitySegmentDTO(
                request.fromCity(),
                "出发地",
                List.of(),
                sameCity(request.fromCity(), request.toCity()) ? "本次为同城或本地出行。" : "请预留从出发地前往主目的地的城际交通时间，票价以官方平台为准。",
                sameCity(request.fromCity(), request.toCity()) ? 0 : 1,
                true));
        segments.add(new CitySegmentDTO(
                request.toCity(),
                "主目的地",
                destinationSpots,
                "每日行程只会优先安排主目的地同城景点，避免把外地景点硬塞进当天游览。",
                Math.max(1, (int) Math.ceil(Math.max(1, destinationSpots.size()) / 3.0)),
                true));

        Map<String, List<SpotCardDTO>> byCity = new LinkedHashMap<>();
        for (SpotCardDTO spot : crossCitySpots) {
            byCity.computeIfAbsent(blankOrDefault(spot.cityName(), "未知城市"), key -> new ArrayList<>()).add(spot);
        }
        byCity.forEach((cityName, spots) -> segments.add(new CitySegmentDTO(
                cityName,
                "跨城市扩展",
                spots,
                "该城市不属于主目的地，系统不会强行安排进 " + request.toCity() + " 的上午/下午/晚上行程。建议增加天数，或作为单独城市旅行计划。",
                Math.max(1, (int) Math.ceil(spots.size() / 3.0)),
                request.days() >= recommendedDaysFor(request, destinationSpots, crossCitySpots))));
        if (!sameCity(request.fromCity(), request.toCity())) {
            segments.add(new CitySegmentDTO(
                    request.fromCity(),
                    "返程",
                    List.of(),
                    "返程交通时间和价格请以 12306、航司或合规售票平台为准。",
                    0,
                    true));
        }
        return segments;
    }

    private FeasibilityDTO evaluateFeasibility(PlanGenerateRequest request, List<SpotCardDTO> selectedSpots, BudgetSummaryDTO budgetSummary, Map<String, ApiResult> data) {
        int days = Math.max(1, request.days());
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        boolean crossCityTrip = !sameCity(request.fromCity(), request.toCity());
        List<SpotCardDTO> destinationSelected = destinationSpots(request, selectedSpots);
        List<SpotCardDTO> crossCitySelected = crossCitySpots(request, selectedSpots);
        long crossCitySpotCount = selectedSpots.stream()
                .filter(spot -> spot.cityName() != null && !sameCity(spot.cityName(), request.toCity()))
                .count();
        long crossCityCount = selectedSpots.stream()
                .map(SpotCardDTO::cityName)
                .filter(city -> city != null && !sameCity(city, request.toCity()))
                .distinct()
                .count();

        int recommendedDays = recommendedDaysFor(request, destinationSelected, crossCitySelected);

        if (days <= 1 && crossCityTrip) {
            warnings.add("1 天内完成 " + request.fromCity() + " 到 " + request.toCity() + " 往返旅游时间较紧，建议至少安排 2-3 天。");
            suggestions.add("建议将行程增加到 3-4 天，或改为目的地本地一日游。");
            recommendedDays = Math.max(recommendedDays, 3);
        }
        if (days <= 1 && crossCitySpotCount > 0) {
            warnings.add("你选择了跨城市景点，1 天内完成多城市游览不现实，建议增加到 3 天以上。");
            suggestions.add("建议只保留主目的地本地景点，或把跨城市景点拆成单独旅行；系统不会把这些外地景点硬塞进当天行程。");
            recommendedDays = Math.max(recommendedDays, 4);
        }
        if (days == 2 && crossCityTrip && crossCitySpotCount > 0) {
            warnings.add("2 天跨城往返且包含跨城市景点会比较紧张，建议减少景点或增加天数。");
            suggestions.add("建议优先安排主目的地景点，跨城市景点作为备选。");
        }
        if (selectedSpots.size() > days * 3) {
            warnings.add("当前选择景点较多，平均每天超过 3 个，可能导致行程过满。");
            suggestions.add("建议减少景点或增加旅游天数。");
            recommendedDays = Math.max(recommendedDays, recommendedDaysFor(request, destinationSelected, crossCitySelected));
        }
        if (crossCityCount >= 2) {
            warnings.add("当前已选择 " + crossCityCount + " 个跨城市扩展目的地，这已经接近多城市长线旅行。");
            suggestions.add("建议按城市拆分为多个独立计划，或至少为每个扩展城市预留 1 天。");
            recommendedDays = Math.max(recommendedDays, days + (int) crossCityCount);
        }
        for (SpotCardDTO spot : selectedSpots) {
            if (spot.cityName() != null && !sameCity(spot.cityName(), request.toCity())) {
                warnings.add("你选择的「" + spot.name() + "」位于 " + spot.cityName()
                        + "，与目的地 " + request.toCity() + " 不在同一城市，会增加额外交通时间。");
            }
        }
        if (budgetSummary.remainingBudget().compareTo(BigDecimal.ZERO) < 0) {
            warnings.add("当前预算低于餐饮、市内交通和住宿建议的规则估算，预算可能偏紧。");
            suggestions.add("建议提高预算，或缩短天数、减少住宿晚数。");
        }
        if (!data.get("hotels").realData() || !data.get("airports").realData()) {
            warnings.add("住宿和机场仅使用高德 POI 位置数据；实时房价、房态、航班班次和机票价格请以官方或正规平台为准。");
            suggestions.add("建议先通过酒店预订平台、航空公司或 12306 确认最终时间和价格。");
        }

        String level;
        boolean feasible;
        if (days < recommendedDays || (days <= 1 && (crossCityTrip || crossCitySpotCount > 0))) {
            level = "不建议";
            feasible = false;
        } else if (!warnings.isEmpty()) {
            level = "偏紧";
            feasible = true;
        } else {
            level = "合理";
            feasible = true;
            warnings.add("当前天数、目的地和景点数量整体匹配。");
            suggestions.add("建议出行前再次核验天气、开放时间和官方交通信息。");
        }
        return new FeasibilityDTO(feasible, level, recommendedDays, warnings, suggestions);
    }

    private int recommendedDaysFor(PlanGenerateRequest request, List<SpotCardDTO> destinationSpots, List<SpotCardDTO> crossCitySpots) {
        int inputDays = Math.max(1, request.days());
        int sameCitySpotDays = Math.max(1, (int) Math.ceil(destinationSpots.size() / 3.0));
        int transportBuffer = sameCity(request.fromCity(), request.toCity()) ? 0 : 1;
        long crossCityCount = crossCitySpots.stream()
                .map(SpotCardDTO::cityName)
                .filter(city -> city != null && !city.isBlank())
                .distinct()
                .count();
        int crossCityBuffer = (int) crossCityCount;
        int extraSpotBuffer = Math.max(0, (int) Math.ceil(Math.max(0, destinationSpots.size() - inputDays * 3) / 3.0));
        int recommended = sameCitySpotDays + transportBuffer + crossCityBuffer + extraSpotBuffer;
        if (!crossCitySpots.isEmpty() && inputDays <= 2) {
            recommended = Math.max(recommended, 4);
        }
        if (!sameCity(request.fromCity(), request.toCity()) && inputDays <= 1) {
            recommended = Math.max(recommended, 3);
        }
        return Math.max(inputDays, recommended);
    }

    private List<DailyPlanDTO> dailyPlans(PlanGenerateRequest request, List<SpotCardDTO> spots) {
        int days = Math.max(1, request.days());
        List<DailyPlanDTO> plans = new ArrayList<>();
        int spotIndex = 0;
        for (int day = 1; day <= days; day++) {
            List<PlanActivityDTO> activities = new ArrayList<>();
            LocalDate date = request.departureDate().plusDays(day - 1L);
            if (days == 1) {
                activities.add(new PlanActivityDTO("上午", request.fromCity() + "出发前往" + request.toCity(),
                        "优先确认真实交通时间，避免单日行程过满。", "一日游应把交通可靠性放在第一位。", "交通费用请以官方购票平台为准。"));
                activities.add(activity("下午", nextSpot(spots, spotIndex++), request.toCity(), "单日行程适合选择一个代表性景点深度体验。"));
                activities.add(new PlanActivityDTO("晚上", request.toCity() + "返回" + request.fromCity(),
                        "预留返程时间，最终车次或航班以官方平台为准。", "返程不能依赖估算票价。", "返程票价请以官方平台实时结果为准。"));
            } else if (day == 1) {
                activities.add(new PlanActivityDTO("上午", request.fromCity() + "出发前往" + request.toCity(),
                        "预留城际交通时间，抵达后以轻量游览为主。", "第一天适合降低行程强度。", "交通费用请以官方购票平台为准。"));
                activities.add(activity("下午", nextSpot(spots, spotIndex++), request.toCity(), "抵达后安排一个交通便利的目的地。"));
                activities.add(new PlanActivityDTO("晚上", request.toCity() + "城市漫步 / 美食体验",
                        "选择酒店或核心商圈周边用餐，减少长距离移动。", "夜间更适合轻松体验本地生活。", "餐饮为规则估算，每人每天 120 元。"));
            } else if (day == days) {
                activities.add(activity("上午", nextSpot(spots, spotIndex++), request.toCity(), "返程日前半天适合安排一个核心景点。"));
                activities.add(new PlanActivityDTO("下午", request.toCity() + "返回" + request.fromCity(),
                        "预留取行李、进站或到机场时间，避免行程过紧。", "最后一天以返程可靠性为优先。", "返程票价请以官方平台实时结果为准。"));
                activities.add(new PlanActivityDTO("晚上", "抵达" + request.fromCity(),
                        "结束本次" + request.toCity() + "旅行。", "保留弹性时间，应对交通延误。", "无固定费用估算。"));
            } else {
                activities.add(activity("上午", nextSpot(spots, spotIndex++), request.toCity(), "上午适合安排代表性景点。"));
                activities.add(activity("下午", nextSpot(spots, spotIndex++), request.toCity(), "下午安排同城景点，注意控制移动距离。"));
                activities.add(new PlanActivityDTO("晚上", request.toCity() + "夜间休闲",
                        "结合当天体力选择江滨、商圈或酒店周边散步。", "夜间活动以轻松安全为主。", "市内交通为规则估算，每人每天 30 元。"));
            }
            plans.add(new DailyPlanDTO(day, date.toString(), "第 " + day + " 天", activities));
        }
        return plans;
    }

    private PlanActivityDTO activity(String period, SpotCardDTO spot, String city, String fallbackReason) {
        if (spot == null) {
            return new PlanActivityDTO(period, city + "核心区域游览",
                    "根据真实 POI 结果灵活选择目的地。", fallbackReason, "门票以景区官方公告为准。");
        }
        String cityPrefix = spot.cityName() == null || sameCity(spot.cityName(), city) ? "" : "跨城市扩展景点 · ";
        return new PlanActivityDTO(period, cityPrefix + spot.name(), spot.address(),
                spot.recommendationReason(), "门票以景区官方公告为准。");
    }

    private SpotCardDTO nextSpot(List<SpotCardDTO> spots, int index) {
        if (spots == null || spots.isEmpty() || index >= spots.size()) {
            return null;
        }
        return spots.get(index);
    }

    private String summary(PlanGenerateRequest request, Map<String, ApiResult> data, List<SpotCardDTO> selectedSpots) {
        String interests = request.interests() == null || request.interests().isEmpty() ? "综合游览" : String.join("、", request.interests());
        boolean amapOk = data.get("spots").realData() || data.get("weather").realData() || data.get("route").realData();
        String dataText = amapOk ? "系统已调用高德真实 POI、天气或路线数据。" : "当前高德真实数据未完整获取，页面会展示失败原因。";
        String selectionText = selectedSpots.isEmpty()
                ? "未选择候选景点，系统会优先使用目的地 POI 自动推荐。"
                : "你已选择 " + selectedSpots.size() + " 个候选景点，系统会优先纳入计划，但不会自动修改主目的地。";
        return "本计划根据出发地 " + request.fromCity() + "、主目的地 " + request.toCity() + "、"
                + request.days() + " 天行程、" + request.budget() + " 元预算和「" + interests + "」偏好生成。"
                + selectionText + dataText
                + " 若未获取到真实往返交通或酒店报价，相关费用以规则估算和官方查询为准。";
    }

    private String budgetConclusion(PlanGenerateRequest request, BudgetSummaryDTO budget) {
        if (request.budget() == null) {
            return "你尚未填写预算。当前计划会列出非实时预算参考区间，最终费用请以航空公司、12306、酒店或正规预订平台为准。";
        }
        BudgetReferenceDTO reference = budget.budgetReference();
        if (reference == null) {
            return "当前预算为 " + request.budget() + " 元。预算参考为系统规则估算，最终费用请以官方与正规平台为准。";
        }
        return "当前预算为 " + request.budget() + " 元。系统规则预估总预算参考区间约 "
                + reference.estimatedMin() + "-" + reference.estimatedMax() + " 元，典型参考约 "
                + reference.estimatedTypical() + " 元，判断为：" + reference.budgetLevel()
                + "。该结果为非实时预算参考，实际价格以官方与正规平台为准。";
    }

    private List<DataSourceDTO> dataSources(Map<String, ApiResult> data, TrainSearchResponse train) {
        List<DataSourceDTO> sources = new ArrayList<>();
        data.forEach((name, result) -> sources.add(new DataSourceDTO(
                result.sourceName(),
                result.realData() ? "真实接口返回" : result.message(),
                result.fetchedAt().toString())));
        sources.add(new DataSourceDTO(train.provider(), train.realData() ? "真实接口返回" : "官方跳转 / 合规接口预留", ""));
        return sources;
    }

    private boolean sameCity(String left, String right) {
        String a = normalizeCity(left);
        String b = normalizeCity(right);
        return !a.isBlank() && !b.isBlank() && (a.equals(b) || a.contains(b) || b.contains(a));
    }

    private String normalizeCity(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace("市", "")
                .replace("地区", "")
                .replace("自治州", "")
                .replace("特别行政区", "");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String blankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
