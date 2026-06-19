package com.citygo.tourism.service;

import com.citygo.tourism.config.PriceEstimateProperties;
import com.citygo.tourism.dto.BudgetReferenceDTO;
import com.citygo.tourism.dto.PlanGenerateRequest;
import com.citygo.tourism.dto.PriceEstimateDTO;
import com.citygo.tourism.dto.RouteDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PriceEstimateService {
    private static final String MODE = "RULE_ESTIMATED";
    private static final String SOURCE = "CityGo预算规则引擎";
    private static final String NOTICE = "预算参考价为非实时规则估算，仅用于旅行预算规划，实际价格以航空公司、12306、酒店或正规预订平台为准。";
    private final PriceEstimateProperties rules;

    public PriceEstimateService(PriceEstimateProperties rules) {
        this.rules = rules;
    }

    public BudgetReferenceDTO estimate(PlanGenerateRequest request, RouteDTO route) {
        OffsetDateTime now = OffsetDateTime.now();
        BigDecimal distanceKm = distanceKm(route);
        int people = Math.max(1, request.people());
        int days = Math.max(1, request.days());
        int nights = Math.max(0, days - 1);
        int rooms = request.roomCount() == null ? Math.max(1, (people + 1) / 2) : Math.max(1, request.roomCount());
        boolean roundTrip = request.roundTrip() == null || request.roundTrip();

        List<PriceEstimateDTO> items = new ArrayList<>();
        items.add(transportEstimate(request, distanceKm, people, roundTrip, now));
        items.add(hotelEstimate(request, nights, rooms, now));
        items.add(foodEstimate(request, days, people, now));
        items.add(localTransportEstimate(request, days, people, now));
        items.add(ticketReserve(request, people, now));

        BigDecimal min = sum(items, PriceEstimateDTO::minPrice);
        BigDecimal typical = sum(items, PriceEstimateDTO::typicalPrice);
        BigDecimal max = sum(items, PriceEstimateDTO::maxPrice);
        BigDecimal reserve = typical.multiply(valueOr(rules.emergencyReserveRate(), "0.10")).setScale(0, RoundingMode.HALF_UP);
        items.add(simple("EMERGENCY_RESERVE", reserve, reserve, reserve, "总行程", List.of("按典型预算的应急预留比例估算"), "MEDIUM", now));
        min = min.add(reserve);
        typical = typical.add(reserve);
        max = max.add(reserve);

        BigDecimal userBudget = request.budget() == null ? BigDecimal.ZERO : request.budget();
        BigDecimal gap = userBudget.subtract(typical);
        String level = budgetLevel(userBudget, min, typical, max);
        return new BudgetReferenceDTO(
                MODE,
                rules.version(),
                userBudget,
                min,
                typical,
                max,
                gap,
                level,
                budgetSuggestions(level),
                items,
                NOTICE,
                now);
    }

    private PriceEstimateDTO transportEstimate(PlanGenerateRequest request, BigDecimal distanceKm, int people, boolean roundTrip, OffsetDateTime now) {
        String preference = normalize(request.trafficPreference(), "train");
        if (preference.contains("flight") || preference.contains("plane") || preference.contains("飞机")) {
            BigDecimal[] range = flightRange(distanceKm);
            BigDecimal factor = dateFactor(request.departureDate());
            BigDecimal multiplier = BigDecimal.valueOf(people * (roundTrip ? 2L : 1L)).multiply(factor);
            return estimate("FLIGHT", range, multiplier, "单人单程", List.of(
                    "跨城市距离约" + distanceKm.setScale(0, RoundingMode.HALF_UP) + "公里",
                    "机票价格为距离和日期规则预估，并非实时查询结果",
                    roundTrip ? "按往返计算" : "按单程计算",
                    "未包含节假日临时涨价或具体航班差异"), "LOW", now);
        }
        BigDecimal rate = trainRate(request);
        BigDecimal base = distanceKm.multiply(rate);
        BigDecimal multiplier = BigDecimal.valueOf(people * (roundTrip ? 2L : 1L));
        BigDecimal min = base.multiply(new BigDecimal("0.80")).multiply(multiplier);
        BigDecimal typical = base.multiply(multiplier);
        BigDecimal max = base.multiply(new BigDecimal("1.25")).multiply(multiplier);
        return estimate("TRAIN", new BigDecimal[]{min, typical, max}, BigDecimal.ONE, "单人单程", List.of(
                "城市间距离按高德路线或地理距离粗略参考",
                "席别参考系数：" + trainSeatLabel(request.trainSeatType()),
                "实际铁路运营里程可能不同",
                "真实车次、票价和余票请以12306为准"), "LOW", now);
    }

    private PriceEstimateDTO hotelEstimate(PlanGenerateRequest request, int nights, int rooms, OffsetDateTime now) {
        String grade = hotelGrade(request.hotelPreference());
        BigDecimal[] nightly = hotelNightlyRange(request.toCity(), grade);
        BigDecimal multiplier = BigDecimal.valueOf((long) nights * rooms);
        return estimate("HOTEL", nightly, multiplier, "每间每晚", List.of(
                request.toCity() + "城市住宿档次规则：" + gradeLabel(grade),
                "住宿晚数：" + nights + "晚，房间数：" + rooms,
                "酒店名称和位置来自高德，价格为城市和档次规则预估",
                "不代表任一具体酒店的实时房价或房态"), "LOW", now);
    }

    private PriceEstimateDTO foodEstimate(PlanGenerateRequest request, int days, int people, OffsetDateTime now) {
        String level = normalize(request.foodPreference(), "standard");
        BigDecimal[] perDay = range(rules.food() == null ? null : rules.food().levels(), level, new BigDecimal[]{bd(100), bd(180)});
        return estimate("FOOD", perDay, BigDecimal.valueOf((long) days * people), "每人每天", List.of(
                "餐饮档次：" + foodLabel(level),
                "按天数和人数计算",
                "属于CityGo预算规则，不是实时市场报价"), "MEDIUM", now);
    }

    private PriceEstimateDTO localTransportEstimate(PlanGenerateRequest request, int days, int people, OffsetDateTime now) {
        String level = normalize(request.localTransportPreference(), "mixed");
        BigDecimal[] perDay = range(rules.localTransport() == null ? null : rules.localTransport().levels(), level, new BigDecimal[]{bd(50), bd(120)});
        return estimate("LOCAL_TRANSPORT", perDay, BigDecimal.valueOf((long) days * people), "每人每天", List.of(
                "市内交通方式：" + localTransportLabel(level),
                "按天数和人数计算",
                "属于CityGo预算规则，不是实时市场报价"), "MEDIUM", now);
    }

    private PriceEstimateDTO ticketReserve(PlanGenerateRequest request, int people, OffsetDateTime now) {
        BigDecimal min = bd(0);
        BigDecimal typical = bd(100L * people);
        BigDecimal max = bd(300L * people);
        return simple("TICKET_RESERVE", min, typical, max, "总行程", List.of(
                "景点门票预留为预算参考",
                "真实门票以景区官方公告为准"), "LOW", now);
    }

    private PriceEstimateDTO estimate(String category, BigDecimal[] range, BigDecimal multiplier, String unit, List<String> basis, String confidence, OffsetDateTime now) {
        return simple(category,
                money(range[0].multiply(multiplier)),
                money(range[1].multiply(multiplier)),
                money(range[2].multiply(multiplier)),
                unit,
                basis,
                confidence,
                now);
    }

    private PriceEstimateDTO simple(String category, BigDecimal min, BigDecimal typical, BigDecimal max, String unit, List<String> basis, String confidence, OffsetDateTime now) {
        return new PriceEstimateDTO(category, MODE, "CNY", min, typical, max, unit, basis, confidence, SOURCE, NOTICE, now, rules.version());
    }

    private BigDecimal[] flightRange(BigDecimal distanceKm) {
        String band = distanceKm.compareTo(bd(500)) <= 0 ? "short"
                : distanceKm.compareTo(bd(1000)) <= 0 ? "medium"
                : distanceKm.compareTo(bd(2000)) <= 0 ? "long" : "extra-long";
        Map<String, List<BigDecimal>> bands = rules.flight() == null ? null : rules.flight().distanceBands();
        BigDecimal[] pair = pair(bands == null ? null : bands.get(band), defaultFlightPair(band));
        return new BigDecimal[]{pair[0], pair[0].add(pair[1]).divide(bd(2), 0, RoundingMode.HALF_UP), pair[1]};
    }

    private BigDecimal[] defaultFlightPair(String band) {
        return switch (band) {
            case "short" -> new BigDecimal[]{bd(300), bd(600)};
            case "medium" -> new BigDecimal[]{bd(450), bd(1000)};
            case "long" -> new BigDecimal[]{bd(650), bd(1500)};
            default -> new BigDecimal[]{bd(900), bd(2200)};
        };
    }

    private BigDecimal trainRate(PlanGenerateRequest request) {
        String type = normalize(request.trainSeatType(), normalize(request.trafficPreference(), "high-speed-second"));
        PriceEstimateProperties.Train train = rules.train();
        if (type.contains("first")) return valueOr(train == null ? null : train.highSpeedFirstClassRate(), "0.75");
        if (type.contains("hard-sleeper")) return valueOr(train == null ? null : train.normalHardSleeperRate(), "0.35");
        if (type.contains("hard-seat") || type.contains("normal")) return valueOr(train == null ? null : train.normalHardSeatRate(), "0.18");
        return valueOr(train == null ? null : train.highSpeedSecondClassRate(), "0.45");
    }

    private BigDecimal[] hotelNightlyRange(String city, String grade) {
        String tier = cityTier(city);
        Map<String, Map<String, List<BigDecimal>>> tiers = rules.hotel() == null ? null : rules.hotel().tiers();
        List<BigDecimal> values = tiers == null || tiers.get(tier) == null ? null : tiers.get(tier).get(grade);
        if (values == null && rules.hotel() != null && rules.hotel().fallback() != null) {
            values = rules.hotel().fallback().get(grade);
        }
        BigDecimal[] pair = pair(values, new BigDecimal[]{bd(280), bd(520)});
        return new BigDecimal[]{pair[0], pair[0].add(pair[1]).divide(bd(2), 0, RoundingMode.HALF_UP), pair[1]};
    }

    private String cityTier(String city) {
        if (rules.hotel() == null || rules.hotel().cityTiers() == null) return "city-tier-3";
        String normalized = normalizeCity(city);
        for (Map.Entry<String, List<String>> entry : rules.hotel().cityTiers().entrySet()) {
            for (String item : entry.getValue()) {
                if (normalizeCity(item).equals(normalized)) return entry.getKey();
            }
        }
        return "city-tier-3";
    }

    private BigDecimal dateFactor(LocalDate date) {
        BigDecimal factor = BigDecimal.ONE;
        if (date != null && (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)) {
            factor = factor.multiply(valueOr(rules.weekendFactor(), "1.10"));
        }
        if (date != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
            if (days <= 3) factor = factor.multiply(valueOr(rules.shortNoticeFactor(), "1.25"));
            else factor = factor.multiply(valueOr(rules.normalNoticeFactor(), "1.00"));
        }
        return factor;
    }

    private BigDecimal distanceKm(RouteDTO route) {
        if (route != null && route.distanceMeters() != null && route.distanceMeters() > 0) {
            return BigDecimal.valueOf(route.distanceMeters()).divide(bd(1000), 1, RoundingMode.HALF_UP);
        }
        return bd(800);
    }

    private BigDecimal[] range(Map<String, List<BigDecimal>> map, String key, BigDecimal[] fallback) {
        BigDecimal[] pair = pair(map == null ? null : map.get(key), fallback);
        return new BigDecimal[]{pair[0], pair[0].add(pair[1]).divide(bd(2), 0, RoundingMode.HALF_UP), pair[1]};
    }

    private BigDecimal[] pair(List<BigDecimal> values, BigDecimal[] fallback) {
        if (values == null || values.size() < 2) return fallback;
        return new BigDecimal[]{values.get(0), values.get(1)};
    }

    private BigDecimal sum(List<PriceEstimateDTO> items, ValuePicker picker) {
        return items.stream().map(picker::value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal valueOr(BigDecimal value, String fallback) {
        return value == null ? new BigDecimal(fallback) : value;
    }

    private BigDecimal bd(long value) {
        return BigDecimal.valueOf(value);
    }

    private String budgetLevel(BigDecimal userBudget, BigDecimal min, BigDecimal typical, BigDecimal max) {
        if (userBudget == null || userBudget.compareTo(BigDecimal.ZERO) <= 0) return "未填写预算";
        if (userBudget.compareTo(min) < 0) return "预算不足";
        if (userBudget.compareTo(typical) < 0) return "预算偏紧";
        if (userBudget.compareTo(max) <= 0) return "预算适中";
        return "预算较充足";
    }

    private List<String> budgetSuggestions(String level) {
        if ("预算不足".equals(level)) return List.of("减少跨城市扩展景点", "选择经济型住宿", "优先公共交通或提前查询官方票价");
        if ("预算偏紧".equals(level)) return List.of("保留应急预算", "调整住宿档次或减少住宿晚数", "提前通过官方平台确认交通费用");
        if ("预算适中".equals(level)) return List.of("出行前复核官方票价和酒店房价", "保留少量浮动预算");
        if ("预算较充足".equals(level)) return List.of("可以保留更高应急预留", "仍需以官方与正规平台价格为准");
        return List.of("填写总预算后可判断预算充足度");
    }

    private String hotelGrade(String value) {
        String normalized = normalize(value, "comfort");
        if (normalized.contains("luxury")) return "luxury";
        if (normalized.contains("upscale")) return "upscale";
        if (normalized.contains("budget") || normalized.contains("economy")) return "economy";
        return "comfort";
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toLowerCase();
    }

    private String normalizeCity(String value) {
        return value == null ? "" : value.replace("市", "").trim();
    }

    private String trainSeatLabel(String value) {
        String type = normalize(value, "high-speed-second");
        if (type.contains("first")) return "高铁/动车一等座参考系数";
        if (type.contains("hard-sleeper")) return "普通列车硬卧参考系数";
        if (type.contains("hard-seat") || type.contains("normal")) return "普通列车硬座参考系数";
        return "高铁/动车二等座参考系数";
    }

    private String gradeLabel(String grade) {
        return switch (grade) {
            case "economy" -> "经济型";
            case "upscale" -> "高档型";
            case "luxury" -> "豪华型";
            default -> "舒适型";
        };
    }

    private String foodLabel(String level) {
        return switch (level) {
            case "economy" -> "经济型";
            case "quality" -> "品质型";
            default -> "普通型";
        };
    }

    private String localTransportLabel(String level) {
        return switch (level) {
            case "public" -> "公共交通为主";
            case "taxi" -> "出租车/网约车为主";
            default -> "混合出行";
        };
    }

    private interface ValuePicker {
        BigDecimal value(PriceEstimateDTO item);
    }
}
