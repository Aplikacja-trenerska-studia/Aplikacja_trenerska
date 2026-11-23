package com.studia.aplikacja_trenerska;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
class StatsApiIntegrationTests {
    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired JwtService jwtService;
    @MockitoBean JavaMailSender mailSender;
    private long benchId;
    private long pushUpId;
    private String benchName;
    private String pushUpName;
    private LocalDate currentWeek;

    @BeforeEach
    void prepare() {
        cleanup();
        var exercises = jdbc.queryForList("select id from exercises order by id", Long.class);
        benchId = exercises.get(0);
        pushUpId = exercises.get(1);
        benchName = jdbc.queryForObject("select name from exercises where id = ?", String.class, benchId);
        pushUpName = jdbc.queryForObject("select name from exercises where id = ?", String.class, pushUpId);
        currentWeek = LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    @AfterEach
    void cleanup() { jdbc.update("delete from users"); }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void weeklyVolumeGroupsSessionsByLocalWeekAndRecordsCoverWholeHistory(UserRole role) throws Exception {
        String token = token("owner@stats.example", role);
        seed(userId("owner@stats.example"));
        String otherToken = token("other@stats.example", UserRole.USER);
        result(userId("other@stats.example"), benchId, benchName, at(currentWeek, 9), 1, 1, "500");
        mvc.perform(auth(get("/api/stats/summary").param("weeks", "12").param("zone", "Europe/Warsaw"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("zone").value("Europe/Warsaw"))
                .andExpect(jsonPath("from").value(currentWeek.minusWeeks(11).toString()))
                .andExpect(jsonPath("to").value(currentWeek.plusDays(6).toString()))
                .andExpect(jsonPath("weeks", hasSize(12)))
                .andExpect(jsonPath("weeks[11].weekStart").value(currentWeek.toString()))
                .andExpect(jsonPath("weeks[11].sessions").value(1))
                .andExpect(jsonPath("weeks[11].sets").value(3))
                .andExpect(jsonPath("weeks[11].reps").value(20))
                .andExpect(jsonPath("weeks[11].volume").value(830.0))
                .andExpect(jsonPath("weeks[10].sessions").value(1))
                .andExpect(jsonPath("weeks[10].volume").value(900.0))
                .andExpect(jsonPath("weeks[0].sessions").value(0))
                .andExpect(jsonPath("weeks[0].volume").value(0.0))
                .andExpect(jsonPath("summary.sessions").value(2))
                .andExpect(jsonPath("summary.sets").value(4))
                .andExpect(jsonPath("summary.reps").value(30))
                .andExpect(jsonPath("summary.volume").value(1730.0))
                .andExpect(jsonPath("summary.activeWeeks").value(2))
                .andExpect(jsonPath("summary.averageSessionsPerWeek").value(0.2))
                .andExpect(jsonPath("records", hasSize(2)))
                .andExpect(jsonPath("$.records[?(@.exerciseId == " + benchId + ")].maxWeight").value(contains(115.0)))
                .andExpect(jsonPath("$.records[?(@.exerciseId == " + benchId + ")].maxWeightReps").value(contains(1)))
                .andExpect(jsonPath("$.records[?(@.exerciseId == " + benchId + ")].estimatedOneRepMax").value(contains(121.0)))
                .andExpect(jsonPath("$.records[?(@.exerciseId == " + benchId + ")].maxReps").value(contains(10)))
                .andExpect(jsonPath("$.records[?(@.exerciseId == " + benchId + ")].totalSets").value(contains(4)))
                .andExpect(jsonPath("$.records[?(@.exerciseId == " + pushUpId + ")].maxWeight").value(contains((Object) null)))
                .andExpect(jsonPath("$.records[?(@.exerciseId == " + pushUpId + ")].estimatedOneRepMax").value(contains((Object) null)))
                .andExpect(jsonPath("$.records[?(@.exerciseId == " + pushUpId + ")].maxReps").value(contains(12)));
        mvc.perform(auth(get("/api/stats/summary"), otherToken)).andExpect(status().isOk())
                .andExpect(jsonPath("summary.sessions").value(1)).andExpect(jsonPath("records", hasSize(1)))
                .andExpect(jsonPath("records[0].maxWeight").value(500.0));
    }

    @Test
    void recentSessionsAreAggregatedNewestFirstWithLimit() throws Exception {
        String token = token("owner@stats.example", UserRole.USER);
        seed(userId("owner@stats.example"));
        mvc.perform(auth(get("/api/stats/sessions").param("limit", "2"), token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].time").value(at(currentWeek, 10).toString()))
                .andExpect(jsonPath("$[0].planName").value("Plan statystyk"))
                .andExpect(jsonPath("$[0].dayName").value("Dzień A"))
                .andExpect(jsonPath("$[0].exercises").value(2))
                .andExpect(jsonPath("$[0].sets").value(3))
                .andExpect(jsonPath("$[0].reps").value(20))
                .andExpect(jsonPath("$[0].volume").value(830.0))
                .andExpect(jsonPath("$[1].time").value(at(currentWeek.minusWeeks(1), 10).toString()));
        mvc.perform(auth(get("/api/stats/sessions"), token)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void emptyHistoryReturnsZeroWeeksAndNoRecords() throws Exception {
        String token = token("owner@stats.example", UserRole.USER);
        mvc.perform(auth(get("/api/stats/summary").param("weeks", "4"), token)).andExpect(status().isOk())
                .andExpect(jsonPath("weeks", hasSize(4))).andExpect(jsonPath("summary.sessions").value(0))
                .andExpect(jsonPath("summary.volume").value(0.0)).andExpect(jsonPath("summary.averageSessionsPerWeek").value(0.0))
                .andExpect(jsonPath("records", hasSize(0)));
        mvc.perform(auth(get("/api/stats/sessions"), token)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsInvalidParameters() throws Exception {
        String token = token("owner@stats.example", UserRole.USER);
        for (String weeks : new String[] {"0", "53", "x"}) {
            mvc.perform(auth(get("/api/stats/summary").param("weeks", weeks), token)).andExpect(status().isBadRequest());
        }
        mvc.perform(auth(get("/api/stats/summary").param("zone", "Mars/Olympus"), token)).andExpect(status().isBadRequest());
        for (String limit : new String[] {"0", "21", "x"}) {
            mvc.perform(auth(get("/api/stats/sessions").param("limit", limit), token)).andExpect(status().isBadRequest());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid", "disabled", "revoked"})
    void statisticsRequireAnActiveSession(String state) throws Exception {
        String token = state.equals("missing") ? null : state.equals("invalid") ? "invalid" : token("owner@stats.example", UserRole.USER);
        if (state.equals("disabled")) jdbc.update("update users set enabled = false");
        if (state.equals("revoked")) jdbc.update("update users set token_version = token_version + 1");
        mvc.perform(auth(get("/api/stats/summary"), token)).andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/stats/sessions"), token)).andExpect(status().isUnauthorized());
    }

    private void seed(long userId) {
        Instant current = at(currentWeek, 10);
        result(userId, benchId, benchName, current, 1, 5, "100");
        result(userId, benchId, benchName, current, 2, 3, "110");
        result(userId, pushUpId, pushUpName, current, 1, 12, "0");
        result(userId, benchId, benchName, at(currentWeek.minusWeeks(1), 10), 1, 10, "90");
        result(userId, benchId, benchName, at(currentWeek.minusWeeks(20), 10), 1, 1, "115");
    }

    private Instant at(LocalDate day, int hour) {
        return day.atTime(hour, 0).atZone(ZONE).toInstant();
    }

    private void result(long userId, long exerciseId, String name, Instant time, int setNo, int reps, String weight) {
        jdbc.update("insert into exercise_results (id,time,user_id,exercise_id,exercise_name,plan_name,day_name,set_no,reps,weight)"
                + " values (?,?,?,?,?,?,?,?,?,?)", UUID.randomUUID(), Timestamp.from(time), userId, exerciseId, name,
                "Plan statystyk", "Dzień A", setNo, reps, new BigDecimal(weight));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        if (token != null) request.header("Authorization", "Bearer " + token);
        return request.contentType(MediaType.APPLICATION_JSON);
    }

    private String token(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName("Stats test");
        user.setPasswordHash("unused-in-api-tests");
        user.setRole(role);
        return jwtService.generateToken(users.saveAndFlush(user));
    }

    private long userId(String email) { return users.findByEmailIgnoreCase(email).orElseThrow().getId(); }
}
