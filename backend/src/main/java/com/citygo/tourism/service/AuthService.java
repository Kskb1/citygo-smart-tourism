package com.citygo.tourism.service;

import com.citygo.tourism.config.SecurityProperties;
import com.citygo.tourism.dto.AuthRequest;
import com.citygo.tourism.dto.AuthResponse;
import com.citygo.tourism.dto.RegisterRequest;
import com.citygo.tourism.dto.UserDTO;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final JdbcTemplate jdbcTemplate;
    private final SecurityProperties securityProperties;

    public AuthService(
            PasswordService passwordService,
            JwtService jwtService,
            JdbcTemplate jdbcTemplate,
            SecurityProperties securityProperties) {
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.jdbcTemplate = jdbcTemplate;
        this.securityProperties = securityProperties;
    }

    @PostConstruct
    void initUsers() {
        try {
            ensureUsersTable();
            migrateLegacyUserTable();
            ensureConfiguredAdmin();
            if (securityProperties.demoUsersEnabled()) {
                ensureUser("user", "123456", "USER", "user@citygo.local", "", false);
                ensureUser("admin", "123456", "ADMIN", "admin@citygo.local", "", false);
            }
        } catch (DataAccessException ignored) {
            // API methods return 503 if the database is not available.
        }
    }

    public AuthResponse login(AuthRequest request) {
        StoredUser user = findByUsername(normalize(request.username()));
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.status()) || !passwordService.matches(request.password(), user.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码不正确。");
        }
        if (passwordService.shouldUpgrade(user.password())) {
            updatePasswordHash(user.id(), request.password());
        }
        String token = jwtService.createToken(user.id(), user.username(), user.role());
        return new AuthResponse(token, toDto(user));
    }

    public UserDTO register(RegisterRequest request) {
        String username = normalize(request.username());
        if (username.isBlank() || request.password() == null || request.password().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名不能为空，密码至少 6 位。");
        }
        try {
            if (findByUsername(username) != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在。");
            }
            return toDto(createUser(username, request.password(), "USER", request.email(), request.phone()));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (DataAccessException e) {
            throw databaseUnavailable(e);
        }
    }

    public UserDTO profile(String authorization) {
        return currentUser(authorization);
    }

    public UserDTO currentUser(String authorization) {
        return toDto(requireUser(authorization));
    }

    public void requireAdmin(String authorization) {
        StoredUser user = requireUser(authorization);
        if (!"ADMIN".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限访问后台管理。");
        }
    }

    public List<UserDTO> users() {
        try {
            return jdbcTemplate.query("""
                            SELECT id, username, password_hash, role,
                                   COALESCE(email, '') AS email,
                                   COALESCE(phone, '') AS phone,
                                   COALESCE(status, 'ACTIVE') AS status
                            FROM users
                            ORDER BY id
                            """, userMapper())
                    .stream()
                    .sorted(Comparator.comparingLong(StoredUser::id))
                    .map(this::toDto)
                    .toList();
        } catch (DataAccessException e) {
            throw databaseUnavailable(e);
        }
    }

    public long countByRole(String role) {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE role = ?", Long.class, role);
            return count == null ? 0 : count;
        } catch (DataAccessException e) {
            return 0;
        }
    }

    private StoredUser requireUser(String authorization) {
        JwtService.Claims claims = jwtService.parse(authorization);
        StoredUser user = findById(claims.userId());
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态无效，请重新登录。");
        }
        return user;
    }

    private StoredUser createUser(String username, String password, String role, String email, String phone) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement("""
                        INSERT INTO users (username, password_hash, role, email, phone, status)
                        VALUES (?, ?, ?, ?, ?, 'ACTIVE')
                        """, new String[]{"id"});
                ps.setString(1, normalize(username));
                ps.setString(2, passwordService.encode(password));
                ps.setString(3, role);
                ps.setString(4, email == null ? "" : email);
                ps.setString(5, phone == null ? "" : phone);
                return ps;
            }, keyHolder);
            long id = keyHolder.getKey() == null ? 0L : keyHolder.getKey().longValue();
            return new StoredUser(id, normalize(username), "", role, email == null ? "" : email, phone == null ? "" : phone, "ACTIVE");
        } catch (DataAccessException e) {
            throw databaseUnavailable(e);
        }
    }

    private void ensureUsersTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  username VARCHAR(64) NOT NULL UNIQUE,
                  password_hash VARCHAR(255) NOT NULL,
                  role VARCHAR(32) NOT NULL,
                  email VARCHAR(128),
                  phone VARCHAR(32),
                  status VARCHAR(32) DEFAULT 'ACTIVE',
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);
    }

    private void migrateLegacyUserTable() {
        try {
            List<StoredUser> legacy = jdbcTemplate.query("""
                    SELECT id, username, password_hash, role,
                           COALESCE(email, '') AS email,
                           COALESCE(phone, '') AS phone,
                           COALESCE(status, 'ACTIVE') AS status
                    FROM `user`
                    ORDER BY id
                    """, userMapper());
            for (StoredUser user : legacy) {
                if (findByUsername(user.username()) == null) {
                    jdbcTemplate.update("""
                            INSERT INTO users (username, password_hash, role, email, phone, status)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """, user.username(), user.password(), user.role(), user.email(), user.phone(), user.status());
                }
            }
        } catch (DataAccessException ignored) {
            // Legacy table is optional.
        }
    }

    private void ensureConfiguredAdmin() {
        String adminPassword = securityProperties.adminPassword();
        boolean hasConfiguredPassword = adminPassword != null && !adminPassword.isBlank();
        String password = hasConfiguredPassword ? adminPassword : "123456";
        ensureUser(
                securityProperties.effectiveAdminUsername(),
                password,
                "ADMIN",
                securityProperties.effectiveAdminEmail(),
                "",
                hasConfiguredPassword);
    }

    private void ensureUser(String username, String password, String role, String email, String phone, boolean updatePassword) {
        StoredUser existing = findByUsername(normalize(username));
        if (existing == null) {
            createUser(username, password, role, email, phone);
        } else if (updatePassword) {
            jdbcTemplate.update("""
                    UPDATE users
                    SET password_hash = ?, role = ?, email = ?, phone = ?, status = 'ACTIVE'
                    WHERE username = ?
                    """, passwordService.encode(password), role, email == null ? "" : email, phone == null ? "" : phone, normalize(username));
        }
    }

    private StoredUser findByUsername(String username) {
        try {
            List<StoredUser> users = jdbcTemplate.query("""
                    SELECT id, username, password_hash, role,
                           COALESCE(email, '') AS email,
                           COALESCE(phone, '') AS phone,
                           COALESCE(status, 'ACTIVE') AS status
                    FROM users
                    WHERE username = ?
                    """, userMapper(), username);
            return users.isEmpty() ? null : users.get(0);
        } catch (DataAccessException e) {
            throw databaseUnavailable(e);
        }
    }

    private StoredUser findById(long id) {
        try {
            List<StoredUser> users = jdbcTemplate.query("""
                    SELECT id, username, password_hash, role,
                           COALESCE(email, '') AS email,
                           COALESCE(phone, '') AS phone,
                           COALESCE(status, 'ACTIVE') AS status
                    FROM users
                    WHERE id = ?
                    """, userMapper(), id);
            return users.isEmpty() ? null : users.get(0);
        } catch (DataAccessException e) {
            throw databaseUnavailable(e);
        }
    }

    private void updatePasswordHash(long userId, String rawPassword) {
        try {
            jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE id = ?", passwordService.encode(rawPassword), userId);
        } catch (DataAccessException ignored) {
            // Login already succeeded; defer upgrade to the next successful login.
        }
    }

    private RowMapper<StoredUser> userMapper() {
        return (rs, rowNum) -> new StoredUser(
                rs.getLong("id"),
                normalize(rs.getString("username")),
                rs.getString("password_hash"),
                rs.getString("role"),
                nullableString(rs, "email"),
                nullableString(rs, "phone"),
                nullableString(rs, "status"));
    }

    private String nullableString(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return rs.wasNull() ? "" : value;
    }

    private UserDTO toDto(StoredUser user) {
        return new UserDTO(user.id(), user.username(), user.role(), user.email(), user.phone(), user.status());
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim();
    }

    private ResponseStatusException databaseUnavailable(Exception e) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "数据库未就绪，暂时无法完成用户认证。", e);
    }

    private record StoredUser(long id, String username, String password, String role, String email, String phone, String status) {
    }
}
