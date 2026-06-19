package com.citygo.tourism.service;

import com.citygo.tourism.dto.SaveTripRequest;
import com.citygo.tourism.dto.UpdateTripTitleRequest;
import com.citygo.tourism.dto.UserDTO;
import com.citygo.tourism.dto.UserTripDTO;
import com.citygo.tourism.exception.TripPayloadTooLargeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserTripService {
    private static final Logger log = LoggerFactory.getLogger(UserTripService.class);
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final long maxPlanJsonBytes;

    public UserTripService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            AuthService authService,
            @Value("${citygo.trip.max-plan-json-bytes:5242880}") long maxPlanJsonBytes) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.authService = authService;
        this.maxPlanJsonBytes = maxPlanJsonBytes;
    }

    @PostConstruct
    void ensureTable() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS user_trip (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        user_id BIGINT NOT NULL,
                        username VARCHAR(64),
                        title VARCHAR(255) NOT NULL,
                        from_city VARCHAR(100),
                        to_city VARCHAR(100),
                        start_date DATE,
                        days INT,
                        people_count INT,
                        budget DECIMAL(12,2),
                        feasibility_level VARCHAR(50),
                        recommended_days INT,
                        summary TEXT,
                        price_mode VARCHAR(50),
                        price_rule_version VARCHAR(50),
                        plan_json LONGTEXT NOT NULL,
                        status VARCHAR(30) DEFAULT 'ACTIVE',
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        INDEX idx_user_trip_user (user_id),
                        INDEX idx_user_trip_created (created_at)
                    )
                    """);
            try {
                jdbcTemplate.execute("ALTER TABLE user_trip ADD COLUMN username VARCHAR(64) NULL AFTER user_id");
            } catch (DataAccessException ignored) {
                // Column already exists or database is unavailable.
            }
            addColumnIfMissing("ALTER TABLE user_trip ADD COLUMN price_mode VARCHAR(50) NULL AFTER summary");
            addColumnIfMissing("ALTER TABLE user_trip ADD COLUMN price_rule_version VARCHAR(50) NULL AFTER price_mode");
        } catch (DataAccessException ignored) {
            // The service returns a clear 503 from API methods if the database is not available.
        }
    }

    public Map<String, Object> save(String authorization, SaveTripRequest request) {
        UserDTO user = authService.currentUser(authorization);
        String planJson = toJson(request.planData());
        long planJsonBytes = planJson.getBytes(StandardCharsets.UTF_8).length;
        if (planJsonBytes > maxPlanJsonBytes) {
            log.warn("Trip save payload too large: userId={}, bytes={}, maxBytes={}", user.id(), planJsonBytes, maxPlanJsonBytes);
            throw new TripPayloadTooLargeException(planJsonBytes, maxPlanJsonBytes);
        }
        log.info("Trip save payload accepted: userId={}, bytes={}", user.id(), planJsonBytes);
        String priceMode = firstNonBlank(request.priceMode(), textAt(request.planData(), "/budgetSummary/budgetReference/priceMode"));
        String priceRuleVersion = firstNonBlank(request.priceRuleVersion(), textAt(request.planData(), "/budgetSummary/budgetReference/ruleVersion"));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                        INSERT INTO user_trip
                        (user_id, username, title, from_city, to_city, start_date, days, people_count, budget,
                         feasibility_level, recommended_days, summary, price_mode, price_rule_version, plan_json, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                        """, new String[]{"id"});
                ps.setLong(1, user.id());
                ps.setString(2, user.username());
                ps.setString(3, request.title().trim());
                ps.setString(4, emptyToNull(request.fromCity()));
                ps.setString(5, emptyToNull(request.toCity()));
                if (request.startDate() == null) {
                    ps.setDate(6, null);
                } else {
                    ps.setDate(6, Date.valueOf(request.startDate()));
                }
                ps.setObject(7, request.days());
                ps.setObject(8, request.peopleCount());
                ps.setBigDecimal(9, request.budget());
                ps.setString(10, emptyToNull(request.feasibilityLevel()));
                ps.setObject(11, request.recommendedDays());
                ps.setString(12, emptyToNull(request.summary()));
                ps.setString(13, emptyToNull(priceMode));
                ps.setString(14, emptyToNull(priceRuleVersion));
                ps.setString(15, planJson);
                return ps;
            }, keyHolder);
        } catch (DataAccessException e) {
            throw databaseUnavailable(e);
        }
        Number id = keyHolder.getKey();
        return Map.of("tripId", id == null ? 0L : id.longValue(), "message", "行程保存成功");
    }

    public List<UserTripDTO> listMine(String authorization, String keyword, String fromCity, String toCity, int page, int size) {
        UserDTO user = authService.currentUser(authorization);
        return queryTrips(false, user.id(), keyword, fromCity, toCity, page, size);
    }

    public List<UserTripDTO> listAllForAdmin(String authorization, String keyword, String fromCity, String toCity, int page, int size) {
        authService.requireAdmin(authorization);
        return queryTrips(true, null, keyword, fromCity, toCity, page, size);
    }

    public UserTripDTO detail(String authorization, long id) {
        UserDTO user = authService.currentUser(authorization);
        return detailForUser(id, user.id());
    }

    public UserTripDTO adminDetail(String authorization, long id) {
        authService.requireAdmin(authorization);
        return detailForUser(id, null);
    }

    public void delete(String authorization, long id) {
        UserDTO user = authService.currentUser(authorization);
        try {
            int updated = jdbcTemplate.update("UPDATE user_trip SET status = 'DELETED' WHERE id = ? AND user_id = ? AND status <> 'DELETED'", id, user.id());
            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "行程不存在或无权删除");
            }
        } catch (DataAccessException e) {
            throw databaseUnavailable(e);
        }
    }

    public UserTripDTO updateTitle(String authorization, long id, UpdateTripTitleRequest request) {
        UserDTO user = authService.currentUser(authorization);
        String title = request.title() == null ? "" : request.title().trim();
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "行程标题不能为空");
        }
        try {
            int updated = jdbcTemplate.update("UPDATE user_trip SET title = ? WHERE id = ? AND user_id = ? AND status <> 'DELETED'", title, id, user.id());
            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "行程不存在或无权修改");
            }
            return detailForUser(id, user.id());
        } catch (DataAccessException e) {
            throw databaseUnavailable(e);
        }
    }

    public long activeCount() {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_trip WHERE status <> 'DELETED'", Long.class);
            return count == null ? 0 : count;
        } catch (DataAccessException e) {
            return 0;
        }
    }

    public long todayCount() {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_trip WHERE status <> 'DELETED' AND DATE(created_at) = CURRENT_DATE", Long.class);
            return count == null ? 0 : count;
        } catch (DataAccessException e) {
            return 0;
        }
    }

    public List<UserTripDTO> recent(int size) {
        return queryTrips(true, null, null, null, null, 1, size);
    }

    private List<UserTripDTO> queryTrips(boolean includeUsername, Long userId, String keyword, String fromCity, String toCity, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT t.*, t.username AS owner_username
                FROM user_trip t
                WHERE t.status <> 'DELETED'
                """);
        if (includeUsername) {
            sql = new StringBuilder("""
                    SELECT t.*, t.username AS owner_username
                    FROM user_trip t
                    WHERE t.status <> 'DELETED'
                    """);
        }
        if (userId != null) {
            sql.append(" AND t.user_id = ?");
            params.add(userId);
        }
        appendLike(sql, params, "t.title", keyword);
        appendLike(sql, params, "t.from_city", fromCity);
        appendLike(sql, params, "t.to_city", toCity);
        sql.append(" ORDER BY t.created_at DESC LIMIT ? OFFSET ?");
        params.add(safeSize);
        params.add((safePage - 1) * safeSize);
        try {
            return jdbcTemplate.query(sql.toString(), tripMapper(false), params.toArray());
        } catch (DataAccessException e) {
            throw databaseUnavailable(e);
        }
    }

    private UserTripDTO detailForUser(long id, Long userId) {
        String sql = "SELECT t.*, t.username AS owner_username FROM user_trip t WHERE t.id = ? AND t.status <> 'DELETED'";
        List<Object> params = new ArrayList<>();
        params.add(id);
        if (userId != null) {
            sql += " AND t.user_id = ?";
            params.add(userId);
        }
        try {
            List<UserTripDTO> trips = jdbcTemplate.query(sql, tripMapper(true), params.toArray());
            if (trips.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "行程不存在或无权访问");
            }
            return trips.get(0);
        } catch (DataAccessException e) {
            throw databaseUnavailable(e);
        }
    }

    private RowMapper<UserTripDTO> tripMapper(boolean includePlanData) {
        return (rs, rowNum) -> new UserTripDTO(
                rs.getLong("id"),
                rs.getLong("user_id"),
                nullableString(rs, "owner_username"),
                rs.getString("title"),
                nullableString(rs, "from_city"),
                nullableString(rs, "to_city"),
                nullableDate(rs, "start_date"),
                nullableInteger(rs, "days"),
                nullableInteger(rs, "people_count"),
                rs.getBigDecimal("budget"),
                nullableString(rs, "feasibility_level"),
                nullableInteger(rs, "recommended_days"),
                nullableString(rs, "summary"),
                nullableString(rs, "price_mode"),
                nullableString(rs, "price_rule_version"),
                nullableString(rs, "status"),
                nullableDateTime(rs, "created_at"),
                nullableDateTime(rs, "updated_at"),
                includePlanData ? parsePlanJson(nullableString(rs, "plan_json")) : null);
    }

    private void addColumnIfMissing(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ignored) {
            // Column already exists or database is unavailable.
        }
    }

    private String textAt(JsonNode node, String pointer) {
        if (node == null) {
            return "";
        }
        JsonNode value = node.at(pointer);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private JsonNode parsePlanJson(String value) {
        if (value == null || value.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            return objectMapper.createObjectNode().put("parseWarning", "保存的行程 JSON 无法解析，请重新生成行程。");
        }
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "行程数据格式不正确");
        }
    }

    private void appendLike(StringBuilder sql, List<Object> params, String column, String value) {
        if (value != null && !value.trim().isBlank()) {
            sql.append(" AND ").append(column).append(" LIKE ?");
            params.add("%" + value.trim() + "%");
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.trim().isBlank() ? first.trim() : second;
    }

    private String nullableString(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDate nullableDate(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private OffsetDateTime nullableDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private ResponseStatusException databaseUnavailable(Exception e) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "数据库未就绪，暂时无法读取或保存行程。", e);
    }
}
