package com.citygo.tourism;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndTripFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginReturnsJwtAndProfileWorks() throws Exception {
        String token = login("user", "123456");

        mockMvc.perform(get("/api/auth/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void registerCreatesUserWithUserRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newuser","password":"123456","email":"newuser@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void protectedTripsRequireLoginAndAdminRequiresRole() throws Exception {
        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isUnauthorized());

        String userToken = login("user", "123456");
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveTripUsesCurrentTokenAndIsolatesUsers() throws Exception {
        String userToken = login("user", "123456");
        String otherToken = registerAndLogin("otheruser", "123456");

        MvcResult saved = saveTrip(userToken, "成都到武汉三日计划");

        long tripId = objectMapper.readTree(saved.getResponse().getContentAsString()).get("tripId").asLong();

        mockMvc.perform(get("/api/trips").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("成都到武汉三日计划"))
                .andExpect(jsonPath("$[0].priceMode").value("RULE_ESTIMATED"))
                .andExpect(jsonPath("$[0].priceRuleVersion").value("1.0"));

        mockMvc.perform(get("/api/trips/" + tripId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/trips/" + tripId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAndDeleteTripWorkForOwner() throws Exception {
        String token = login("user", "123456");
        MvcResult saved = saveTrip(token, "待修改行程");
        long tripId = objectMapper.readTree(saved.getResponse().getContentAsString()).get("tripId").asLong();

        mockMvc.perform(put("/api/trips/" + tripId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"已修改行程\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("已修改行程"));

        mockMvc.perform(delete("/api/trips/" + tripId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trips/" + tripId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void oversizedPlanDataReturnsFriendly413() throws Exception {
        String token = login("user", "123456");
        String oversized = "x".repeat(5 * 1024 * 1024 + 10);

        mockMvc.perform(post("/api/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"超大行程",
                                  "fromCity":"成都",
                                  "toCity":"武汉",
                                  "startDate":"2026-07-10",
                                  "days":3,
                                  "peopleCount":1,
                                  "budget":3000,
                                  "priceMode":"RULE_ESTIMATED",
                                  "priceRuleVersion":"1.0",
                                  "planData":{"huge":"%s"}
                                }
                                """.formatted(oversized)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("TRIP_PAYLOAD_TOO_LARGE"))
                .andExpect(jsonPath("$.message").value("行程数据过大，请精简后重新保存。"));
    }

    private String registerAndLogin(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","email":"%s@example.com"}
                                """.formatted(username, password, username)))
                .andExpect(status().isOk());
        return login(username, password);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private MvcResult saveTrip(String token, String title) throws Exception {
        return mockMvc.perform(post("/api/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "fromCity":"成都",
                                  "toCity":"武汉",
                                  "startDate":"2026-07-10",
                                  "days":3,
                                  "peopleCount":1,
                                  "roomCount":1,
                                  "budget":3000,
                                  "feasibilityLevel":"合理",
                                  "recommendedDays":3,
                                  "summary":"测试保存",
                                  "priceMode":"RULE_ESTIMATED",
                                  "priceRuleVersion":"1.0",
                                  "planData":{
                                    "title":"%s",
                                    "budgetSummary":{
                                      "budgetReference":{
                                        "priceMode":"RULE_ESTIMATED",
                                        "ruleVersion":"1.0"
                                      }
                                    }
                                  }
                                }
                                """.formatted(title, title)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").isNumber())
                .andReturn();
    }
}
