package com.studia.aplikacja_trenerska;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.studia.aplikacja_trenerska.auth.service.JwtService;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalculatorApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired JwtService jwtService;
    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    @AfterEach
    void cleanup() { jdbc.update("delete from users"); }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void oneRepMaxUsesEpleyWithComparisonFormulasAndLoadTableForEveryRole(UserRole role) throws Exception {
        mvc.perform(auth(get("/api/calc/1rm").param("weight", "100").param("reps", "5"), token(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("weight").value(100))
                .andExpect(jsonPath("reps").value(5))
                .andExpect(jsonPath("epley").value(116.7))
                .andExpect(jsonPath("brzycki").value(112.5))
                .andExpect(jsonPath("lombardi").value(117.5))
                .andExpect(jsonPath("percentages", hasSize(11)))
                .andExpect(jsonPath("percentages[0].percent").value(100))
                .andExpect(jsonPath("percentages[0].weight").value(116.5))
                .andExpect(jsonPath("percentages[0].reps").value(1))
                .andExpect(jsonPath("percentages[4].percent").value(80))
                .andExpect(jsonPath("percentages[4].weight").value(93.5))
                .andExpect(jsonPath("percentages[4].reps").value(8))
                .andExpect(jsonPath("percentages[10].percent").value(50))
                .andExpect(jsonPath("percentages[10].reps").value(30));
    }

    @Test
    void singleRepetitionReturnsTheLiftedWeightForEveryFormula() throws Exception {
        mvc.perform(auth(get("/api/calc/1rm").param("weight", "82.5").param("reps", "1"), token(UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("epley").value(82.5))
                .andExpect(jsonPath("brzycki").value(82.5))
                .andExpect(jsonPath("lombardi").value(82.5));
    }

    @ParameterizedTest
    @CsvSource({"0,5", "-1,5", "2000.01,5", "1.001,5", "100,0", "100,21", "abc,5", "100,x"})
    void oneRepMaxRejectsInvalidInput(String weight, String reps) throws Exception {
        mvc.perform(auth(get("/api/calc/1rm").param("weight", weight).param("reps", reps), token(UserRole.USER)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").isString());
    }

    @Test
    void oneRepMaxRequiresBothParameters() throws Exception {
        String token = token(UserRole.USER);
        mvc.perform(auth(get("/api/calc/1rm").param("weight", "100"), token)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors.reps").isString());
        mvc.perform(auth(get("/api/calc/1rm").param("reps", "5"), token)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors.weight").isString());
    }

    @Test
    void caloriesUseMifflinStJeorActivityFactorAndMacroSplitForMaintenance() throws Exception {
        mvc.perform(auth(calories(Map.of()), token(UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("bmr").value(1780))
                .andExpect(jsonPath("tdee").value(2759))
                .andExpect(jsonPath("targetCalories").value(2759))
                .andExpect(jsonPath("calorieChange").value(0))
                .andExpect(jsonPath("activityFactor").value(1.55))
                .andExpect(jsonPath("macros.proteinG").value(128))
                .andExpect(jsonPath("macros.fatG").value(77))
                .andExpect(jsonPath("macros.carbsG").value(389))
                .andExpect(jsonPath("macros.proteinKcal").value(512))
                .andExpect(jsonPath("macros.fatKcal").value(693))
                .andExpect(jsonPath("macros.carbsKcal").value(1556));
    }

    @Test
    void caloriesApplyDeficitForWeightLossAndSurplusForGain() throws Exception {
        String token = token(UserRole.USER);
        mvc.perform(auth(calories(Map.of("sex", "FEMALE", "age", "25", "heightCm", "165", "weightKg", "60",
                "activity", "SEDENTARY", "goal", "LOSE")), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("bmr").value(1345))
                .andExpect(jsonPath("tdee").value(1614))
                .andExpect(jsonPath("targetCalories").value(1372))
                .andExpect(jsonPath("calorieChange").value(-242))
                .andExpect(jsonPath("macros.proteinG").value(120))
                .andExpect(jsonPath("macros.fatG").value(38))
                .andExpect(jsonPath("macros.carbsG").value(138));
        mvc.perform(auth(calories(Map.of("goal", "GAIN")), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("targetCalories").value(3035))
                .andExpect(jsonPath("calorieChange").value(greaterThan(0)))
                .andExpect(jsonPath("macros.proteinG").value(144));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sex=OTHER", "age=14", "age=101", "heightCm=99", "heightCm=251", "weightKg=29.99",
            "weightKg=300.01", "weightKg=80.123", "activity=EXTREME", "goal=BULK", "age=abc"})
    void caloriesRejectInvalidInput(String override) throws Exception {
        String[] pair = override.split("=");
        mvc.perform(auth(calories(Map.of(pair[0], pair[1])), token(UserRole.USER)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").isString());
    }

    @Test
    void caloriesRequireEveryParameter() throws Exception {
        mvc.perform(auth(get("/api/calc/calories").param("sex", "MALE"), token(UserRole.USER)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").isString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid", "disabled", "revoked"})
    void calculatorsRequireAnActiveSession(String state) throws Exception {
        String token = state.equals("missing") ? null : state.equals("invalid") ? "invalid" : token(UserRole.USER);
        if (state.equals("disabled")) jdbc.update("update users set enabled = false");
        if (state.equals("revoked")) jdbc.update("update users set token_version = token_version + 1");
        mvc.perform(auth(get("/api/calc/1rm").param("weight", "100").param("reps", "5"), token))
                .andExpect(status().isUnauthorized());
        mvc.perform(auth(calories(Map.of()), token)).andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder calories(Map<String, String> overrides) {
        Map<String, String> params = new LinkedHashMap<>(Map.of("sex", "MALE", "age", "30", "heightCm", "180",
                "weightKg", "80", "activity", "MODERATE", "goal", "MAINTAIN"));
        params.putAll(overrides);
        MockHttpServletRequestBuilder request = get("/api/calc/calories");
        params.forEach(request::param);
        return request;
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        if (token != null) request.header("Authorization", "Bearer " + token);
        return request.contentType(MediaType.APPLICATION_JSON);
    }

    private String token(UserRole role) {
        User user = new User();
        user.setEmail("calc-" + role.name().toLowerCase() + "@calc.example");
        user.setDisplayName("Calculator test");
        user.setPasswordHash("unused-in-api-tests");
        user.setRole(role);
        return jwtService.generateToken(users.saveAndFlush(user));
    }
}
