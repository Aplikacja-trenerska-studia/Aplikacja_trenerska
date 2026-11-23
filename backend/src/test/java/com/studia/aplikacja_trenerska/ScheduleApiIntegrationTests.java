package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.jayway.jsonpath.JsonPath;
import com.studia.aplikacja_trenerska.auth.service.JwtService;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScheduleApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired JwtService jwtService;
    @MockitoBean JavaMailSender mailSender;
    private long exerciseId;

    @BeforeEach
    void prepare() {
        cleanup();
        exerciseId = jdbc.queryForList("select id from exercises order by id", Long.class).getFirst();
    }

    @AfterEach
    void cleanup() { jdbc.update("delete from users"); }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void schedulesWorkoutFromOwnPlanAndListsItInTheRequestedRange(UserRole role) throws Exception {
        String token = token("owner@schedule.example", role);
        Plan plan = plan(token);
        mvc.perform(auth(post("/api/schedule"), token).content(entry("2026-12-01", plan.id(), plan.secondDayId(), "  Rano przed pracą  ")))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("id").isNumber())
                .andExpect(jsonPath("date").value("2026-12-01"))
                .andExpect(jsonPath("planId").value(plan.id()))
                .andExpect(jsonPath("planDayId").value(plan.secondDayId()))
                .andExpect(jsonPath("planName").value("Plan kalendarza"))
                .andExpect(jsonPath("dayName").value("Dzień B"))
                .andExpect(jsonPath("note").value("Rano przed pracą"))
                .andExpect(jsonPath("completed").value(false))
                .andExpect(jsonPath("completedAt").isEmpty())
                .andExpect(jsonPath("userId").doesNotExist());
        create(token, "2026-11-28", plan.id(), plan.firstDayId());
        create(token, "2027-01-15", plan.id(), plan.firstDayId());
        mvc.perform(auth(get("/api/schedule").param("from", "2026-11-28").param("to", "2026-12-31"), token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].date").value("2026-11-28"))
                .andExpect(jsonPath("$[1].date").value("2026-12-01"));
    }

    @Test
    void agendaListsTodayRecentOverdueAndTheNextSevenDays() throws Exception {
        String token = token("owner@schedule.example", UserRole.USER);
        Plan plan = plan(token);
        long today = create(token, "2026-12-10", plan.id(), plan.firstDayId());
        long overdue = create(token, "2026-12-07", plan.id(), plan.firstDayId());
        long doneOverdue = create(token, "2026-12-08", plan.id(), plan.firstDayId());
        create(token, "2026-11-20", plan.id(), plan.firstDayId());
        long upcoming = create(token, "2026-12-12", plan.id(), plan.secondDayId());
        create(token, "2026-12-18", plan.id(), plan.firstDayId());
        mvc.perform(auth(post("/api/schedule/{id}/complete", doneOverdue), token)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/schedule/agenda").param("date", "2026-12-10"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("date").value("2026-12-10"))
                .andExpect(jsonPath("today", hasSize(1))).andExpect(jsonPath("today[0].id").value(today))
                .andExpect(jsonPath("overdue", hasSize(1))).andExpect(jsonPath("overdue[0].id").value(overdue))
                .andExpect(jsonPath("upcoming", hasSize(1))).andExpect(jsonPath("upcoming[0].id").value(upcoming))
                .andExpect(jsonPath("upcoming[0].dayName").value("Dzień B"));
    }

    @Test
    void completionCanBeMarkedRepeatedlyAndReopened() throws Exception {
        String token = token("owner@schedule.example", UserRole.USER);
        Plan plan = plan(token);
        long id = create(token, "2026-12-01", plan.id(), plan.firstDayId());
        String first = mvc.perform(auth(post("/api/schedule/{id}/complete", id), token)).andExpect(status().isOk())
                .andExpect(jsonPath("completed").value(true)).andExpect(jsonPath("completedAt").isString())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(auth(post("/api/schedule/{id}/complete", id), token)).andExpect(status().isOk())
                .andExpect(jsonPath("completedAt").value((String) JsonPath.read(first, "$.completedAt")));
        mvc.perform(auth(delete("/api/schedule/{id}/complete", id), token)).andExpect(status().isOk())
                .andExpect(jsonPath("completed").value(false)).andExpect(jsonPath("completedAt").isEmpty());
    }

    @Test
    void updateMovesTheEntryAndKeepsCompletionWhileDeleteRemovesIt() throws Exception {
        String token = token("owner@schedule.example", UserRole.USER);
        Plan plan = plan(token);
        long id = create(token, "2026-12-01", plan.id(), plan.firstDayId());
        mvc.perform(auth(post("/api/schedule/{id}/complete", id), token)).andExpect(status().isOk());
        mvc.perform(auth(put("/api/schedule/{id}", id), token).content(entry("2026-12-03", plan.id(), plan.secondDayId(), "")))
                .andExpect(status().isOk()).andExpect(jsonPath("date").value("2026-12-03"))
                .andExpect(jsonPath("dayName").value("Dzień B")).andExpect(jsonPath("note").isEmpty())
                .andExpect(jsonPath("completed").value(true));
        mvc.perform(auth(delete("/api/schedule/{id}", id), token)).andExpect(status().isNoContent());
        mvc.perform(auth(delete("/api/schedule/{id}", id), token)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from scheduled_workouts", Integer.class)).isZero();
    }

    @Test
    void entriesFollowPlanEditsByDayPositionAndKeepNamesAfterPlanDeletion() throws Exception {
        String token = token("owner@schedule.example", UserRole.USER);
        Plan plan = plan(token);
        create(token, "2026-12-01", plan.id(), plan.secondDayId());
        String edited = mvc.perform(auth(put("/api/plans/{id}", plan.id()), token).content(planBody("Nowa nazwa", "Nowy A", "Nowy B")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long newSecondDay = ((Number) JsonPath.read(edited, "$.days[1].id")).longValue();
        mvc.perform(auth(get("/api/schedule").param("from", "2026-12-01").param("to", "2026-12-01"), token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].planDayId").value(newSecondDay))
                .andExpect(jsonPath("$[0].planName").value("Nowa nazwa")).andExpect(jsonPath("$[0].dayName").value("Nowy B"));
        mvc.perform(auth(delete("/api/plans/{id}", plan.id()), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/schedule").param("from", "2026-12-01").param("to", "2026-12-01"), token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].planId").isEmpty()).andExpect(jsonPath("$[0].planDayId").isEmpty())
                .andExpect(jsonPath("$[0].planName").value("Nowa nazwa")).andExpect(jsonPath("$[0].dayName").value("Nowy B"));
    }

    @Test
    void entriesAndPlansOfOtherAccountsAreInaccessible() throws Exception {
        String owner = token("owner@schedule.example", UserRole.USER);
        String other = token("other@schedule.example", UserRole.TRAINER);
        Plan plan = plan(owner);
        Plan otherPlan = plan(other);
        long id = create(owner, "2026-12-01", plan.id(), plan.firstDayId());
        mvc.perform(auth(get("/api/schedule").param("from", "2026-12-01").param("to", "2026-12-01"), other))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(auth(put("/api/schedule/{id}", id), other).content(entry("2026-12-02", otherPlan.id(), otherPlan.firstDayId(), null)))
                .andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/schedule/{id}/complete", id), other)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/schedule/{id}", id), other)).andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/schedule"), other).content(entry("2026-12-02", plan.id(), plan.firstDayId(), null)))
                .andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/schedule"), owner).content(entry("2026-12-02", plan.id(), otherPlan.firstDayId(), null)))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from scheduled_workouts", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select completed_at from scheduled_workouts", java.sql.Timestamp.class)).isNull();
    }

    @Test
    void limitsEntriesPerDay() throws Exception {
        String token = token("owner@schedule.example", UserRole.USER);
        Plan plan = plan(token);
        for (int index = 0; index < 10; index++) create(token, "2026-12-01", plan.id(), plan.firstDayId());
        mvc.perform(auth(post("/api/schedule"), token).content(entry("2026-12-01", plan.id(), plan.firstDayId(), null)))
                .andExpect(status().isConflict());
        long moved = create(token, "2026-12-02", plan.id(), plan.firstDayId());
        mvc.perform(auth(put("/api/schedule/{id}", moved), token).content(entry("2026-12-01", plan.id(), plan.firstDayId(), null)))
                .andExpect(status().isConflict());
        mvc.perform(auth(put("/api/schedule/{id}", moved), token).content(entry("2026-12-02", plan.id(), plan.secondDayId(), null)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"planId\":PLAN,\"planDayId\":DAY}", "{\"date\":\"1999-12-31\",\"planId\":PLAN,\"planDayId\":DAY}",
            "{\"date\":\"2101-01-01\",\"planId\":PLAN,\"planDayId\":DAY}", "{\"date\":\"2026-02-30\",\"planId\":PLAN,\"planDayId\":DAY}",
            "{\"date\":\"2026-12-01\",\"planId\":0,\"planDayId\":DAY}", "{\"date\":\"2026-12-01\",\"planId\":PLAN}",
            "{\"date\":\"2026-12-01\",\"planId\":PLAN,\"planDayId\":DAY,\"note\":\"LONG\"}"})
    void rejectsInvalidEntries(String template) throws Exception {
        String token = token("owner@schedule.example", UserRole.USER);
        Plan plan = plan(token);
        String body = template.replace("PLAN", Long.toString(plan.id())).replace("DAY", Long.toString(plan.firstDayId()))
                .replace("LONG", "x".repeat(501));
        mvc.perform(auth(post("/api/schedule"), token).content(body)).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from scheduled_workouts", Integer.class)).isZero();
    }

    @Test
    void rejectsInvalidRanges() throws Exception {
        String token = token("owner@schedule.example", UserRole.USER);
        mvc.perform(auth(get("/api/schedule").param("from", "2026-12-02").param("to", "2026-12-01"), token)).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/schedule").param("from", "2026-01-01").param("to", "2026-04-03"), token)).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/schedule").param("from", "2026-01-01").param("to", "2026-04-02"), token)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/schedule").param("from", "wrong").param("to", "2026-04-01"), token)).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/schedule").param("from", "2026-01-01"), token)).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/schedule/agenda"), token)).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/schedule/agenda").param("date", "1999-01-01"), token)).andExpect(status().isBadRequest());
    }

    @Test
    void deletingTheAccountRemovesItsSchedule() throws Exception {
        String owner = token("owner@schedule.example", UserRole.USER);
        String other = token("other@schedule.example", UserRole.USER);
        Plan plan = plan(owner);
        Plan otherPlan = plan(other);
        create(owner, "2026-12-01", plan.id(), plan.firstDayId());
        create(other, "2026-12-01", otherPlan.id(), otherPlan.firstDayId());
        jdbc.update("delete from users where email = 'owner@schedule.example'");
        assertThat(jdbc.queryForObject("select count(*) from scheduled_workouts", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid", "disabled", "revoked"})
    void everyOperationRequiresAnActiveSession(String state) throws Exception {
        String token = state.equals("missing") ? null : state.equals("invalid") ? "invalid" : token("owner@schedule.example", UserRole.USER);
        if (state.equals("disabled")) jdbc.update("update users set enabled = false");
        if (state.equals("revoked")) jdbc.update("update users set token_version = token_version + 1");
        for (var request : new MockHttpServletRequestBuilder[] {
                get("/api/schedule").param("from", "2026-12-01").param("to", "2026-12-31"),
                get("/api/schedule/agenda").param("date", "2026-12-01"), post("/api/schedule").content("{}"),
                put("/api/schedule/1").content("{}"), post("/api/schedule/1/complete"),
                delete("/api/schedule/1/complete"), delete("/api/schedule/1")}) {
            mvc.perform(auth(request, token)).andExpect(status().isUnauthorized());
        }
    }

    private long create(String token, String date, long planId, long dayId) throws Exception {
        String response = mvc.perform(auth(post("/api/schedule"), token).content(entry(date, planId, dayId, null)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String entry(String date, long planId, long dayId, String note) {
        return "{\"date\":\"" + date + "\",\"planId\":" + planId + ",\"planDayId\":" + dayId
                + (note == null ? "" : ",\"note\":\"" + note + "\"") + "}";
    }

    private Plan plan(String token) throws Exception {
        String response = mvc.perform(auth(post("/api/plans"), token).content(planBody("Plan kalendarza", "Dzień A", "Dzień B")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Plan(((Number) JsonPath.read(response, "$.id")).longValue(),
                ((Number) JsonPath.read(response, "$.days[0].id")).longValue(),
                ((Number) JsonPath.read(response, "$.days[1].id")).longValue());
    }

    private String planBody(String name, String firstDay, String secondDay) {
        String exercises = "\"exercises\":[{\"exerciseId\":" + exerciseId + ",\"sets\":3,\"reps\":10,\"weight\":20}]";
        return "{\"name\":\"" + name + "\",\"days\":[{\"name\":\"" + firstDay + "\"," + exercises + "},{\"name\":\""
                + secondDay + "\"," + exercises + "}]}";
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        if (token != null) request.header("Authorization", "Bearer " + token);
        return request.contentType(MediaType.APPLICATION_JSON);
    }

    private String token(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName("Schedule test");
        user.setPasswordHash("unused-in-api-tests");
        user.setRole(role);
        return jwtService.generateToken(users.saveAndFlush(user));
    }

    private record Plan(long id, long firstDayId, long secondDayId) { }
}
