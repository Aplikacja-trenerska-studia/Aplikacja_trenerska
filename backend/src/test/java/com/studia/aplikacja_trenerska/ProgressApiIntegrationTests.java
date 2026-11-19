package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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

// Every HTTP mutation commits independently; rollback and cascades are checked against persisted data.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProgressApiIntegrationTests {
    private static final String WHEN = "2026-01-15T12:00:00Z";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired JwtService jwtService;
    @MockitoBean JavaMailSender mailSender;
    private long exerciseId;
    private long otherExerciseId;

    @BeforeEach
    void prepare() {
        cleanup();
        var exercises = jdbc.queryForList("select id from exercises order by id", Long.class);
        exerciseId = exercises.get(0);
        otherExerciseId = exercises.get(1);
    }

    @AfterEach
    void cleanup() { jdbc.update("delete from users"); }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void savesWorkoutAndPartialBodyMeasurementForEveryAuthenticatedRole(UserRole role) throws Exception {
        String token = token("owner@progress.example", role);
        Plan plan = plan(token);
        String body = payload(plan).replace("\"time\":\"" + WHEN + "\"", "\"time\":\"2026-01-15T12:00:00.123456789Z\"");
        mvc.perform(auth(post("/api/results"), token).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").isString())
                .andExpect(jsonPath("$[0].time").value("2026-01-15T12:00:00.123456Z"))
                .andExpect(jsonPath("$[0].exerciseId").value(exerciseId))
                .andExpect(jsonPath("$[0].planName").value("Plan progresu"))
                .andExpect(jsonPath("$[0].dayName").value("Pierwszy dzień"))
                .andExpect(jsonPath("$[0].reps").value(8))
                .andExpect(jsonPath("$[0].weight").value(42.75))
                .andExpect(jsonPath("$[0].userId").doesNotExist());
        mvc.perform(auth(post("/api/body-metrics"), token).content(metric()))
                .andExpect(status().isCreated()).andExpect(jsonPath("weightKg").value(82.25))
                .andExpect(jsonPath("waistCm").value(90.5)).andExpect(jsonPath("armCm").isEmpty())
                .andExpect(jsonPath("userId").doesNotExist());
        mvc.perform(auth(get("/api/results"), token)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(auth(get("/api/body-metrics"), token)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        assertThat(jdbc.queryForObject("select weight from exercise_results", java.math.BigDecimal.class))
                .isEqualByComparingTo("42.75");
    }

    @Test
    void resultHistoryRetainsSnapshotsAfterPlanRenameReplacementAndDeletion() throws Exception {
        String token = token("owner@progress.example", UserRole.USER);
        Plan plan = plan(token);
        mvc.perform(auth(post("/api/results"), token).content(payload(plan))).andExpect(status().isCreated());
        String replacement = "{\"name\":\"Changed plan\",\"days\":[{\"name\":\"Changed day\",\"exercises\":["
                + "{\"exerciseId\":" + otherExerciseId + ",\"sets\":1,\"reps\":5,\"weight\":0}]}]}";
        mvc.perform(auth(put("/api/plans/{id}", plan.id()), token).content(replacement)).andExpect(status().isOk());
        mvc.perform(auth(delete("/api/plans/{id}", plan.id()), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/results"), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].planName").value("Plan progresu"))
                .andExpect(jsonPath("$[0].dayName").value("Pierwszy dzień"));
    }

    @Test
    void resultsAndMeasurementsAreScopedToOwnerAndIgnoreSuppliedUserId() throws Exception {
        String owner = token("owner@progress.example", UserRole.USER);
        String other = token("other@progress.example", UserRole.TRAINER);
        long otherId = userId("other@progress.example");
        Plan plan = plan(owner);
        String result = mvc.perform(auth(post("/api/results"), owner)
                .content("{\"userId\":" + otherId + "," + payload(plan).substring(1)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String metric = mvc.perform(auth(post("/api/body-metrics"), owner)
                .content("{\"userId\":" + otherId + "," + metric().substring(1)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mvc.perform(auth(get("/api/results"), other)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(auth(get("/api/body-metrics"), other)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(auth(delete("/api/results/{id}", (String) JsonPath.read(result, "$[0].id")), other).param("time", WHEN))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/body-metrics/{id}", (String) JsonPath.read(metric, "$.id")), other).param("time", WHEN))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select user_id from exercise_results", Long.class)).isEqualTo(userId("owner@progress.example"));
        assertThat(jdbc.queryForObject("select user_id from body_metrics", Long.class)).isEqualTo(userId("owner@progress.example"));
        mvc.perform(auth(post("/api/results"), other).content(payload(plan))).andExpect(status().isNotFound());
    }

    @Test
    void compositeKeysAllowOwnerDeletionUsingReturnedMicrosecondTimestamp() throws Exception {
        String token = token("owner@progress.example", UserRole.USER);
        String result = mvc.perform(auth(post("/api/results"), token)
                .content(payload(plan(token)).replace(WHEN, "2026-01-15T12:00:00.123456789Z")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String resultId = JsonPath.read(result, "$[0].id");
        String time = JsonPath.read(result, "$[0].time");
        mvc.perform(auth(delete("/api/results/{id}", resultId), token).param("time", WHEN)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/results/{id}", resultId), token).param("time", time)).andExpect(status().isNoContent());
        mvc.perform(auth(delete("/api/results/{id}", resultId), token).param("time", time)).andExpect(status().isNotFound());
        String metric = mvc.perform(auth(post("/api/body-metrics"), token).content(metric()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mvc.perform(auth(delete("/api/body-metrics/{id}", (String) JsonPath.read(metric, "$.id")), token)
                .param("time", (String) JsonPath.read(metric, "$.time"))).andExpect(status().isNoContent());
        assertThat(count("exercise_results")).isZero();
        assertThat(count("body_metrics")).isZero();
    }

    @Test
    void inclusiveDateAndExerciseFiltersReturnNewestFirst() throws Exception {
        String token = token("owner@progress.example", UserRole.USER);
        Plan plan = plan(token);
        for (String time : new String[] {"2026-01-14T12:00:00Z", WHEN, "2026-01-16T12:00:00Z"}) {
            mvc.perform(auth(post("/api/results"), token).content(payload(plan).replace(WHEN, time)))
                    .andExpect(status().isCreated());
            mvc.perform(auth(post("/api/body-metrics"), token).content(metric().replace(WHEN, time)))
                    .andExpect(status().isCreated());
        }
        mvc.perform(auth(get("/api/results"), token).param("exerciseId", Long.toString(exerciseId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].time").value("2026-01-16T12:00:00Z"));
        mvc.perform(auth(get("/api/results"), token).param("exerciseId", Long.toString(otherExerciseId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        for (String endpoint : new String[] {"/api/results", "/api/body-metrics"}) {
            mvc.perform(auth(get(endpoint), token).param("from", WHEN).param("to", WHEN))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].time").value(WHEN));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"foreign-exercise", "foreign-day", "duplicate-set", "invalid-last-set"})
    void invalidSessionNeverLeavesPartialResults(String problem) throws Exception {
        String token = token("owner@progress.example", UserRole.USER);
        Plan plan = plan(token);
        String body = payload(plan);
        if (problem.equals("foreign-exercise")) body = body.replace("\"exerciseId\":" + exerciseId, "\"exerciseId\":" + otherExerciseId);
        if (problem.equals("foreign-day")) body = body.replace("\"planDayId\":" + plan.dayId(), "\"planDayId\":" + plan(token).dayId());
        if (problem.equals("duplicate-set")) body = body.replace("\"weight\":42.75}", "\"weight\":42.75}," + set(1, 8, "1"));
        if (problem.equals("invalid-last-set")) body = body.replace("\"weight\":42.75}", "\"weight\":42.75}," + set(2, 0, "1"));
        mvc.perform(auth(post("/api/results"), token).content(body)).andExpect(status().isBadRequest());
        assertThat(count("exercise_results")).isZero();
    }

    @Test
    void savesSeveralSetsAtomicallyAndAllowsSkippedExercises() throws Exception {
        String token = token("owner@progress.example", UserRole.USER);
        Plan plan = plan(token);
        String body = payload(plan).replace("\"weight\":42.75}", "\"weight\":42.75}," + set(2, 6, "45"));
        mvc.perform(auth(post("/api/results"), token).content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[1].setNo").value(2));
        assertThat(count("exercise_results")).isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("invalidSetFields")
    void validatesNestedResultFields(String original, String replacement) throws Exception {
        String token = token("owner@progress.example", UserRole.USER);
        String body = payload(plan(token)).replace(original, replacement);
        mvc.perform(auth(post("/api/results"), token).content(body)).andExpect(status().isBadRequest());
        assertThat(count("exercise_results")).isZero();
    }

    static Stream<Arguments> invalidSetFields() {
        return Stream.of(
                Arguments.of("\"setNo\":1", "\"setNo\":0"),
                Arguments.of("\"setNo\":1", "\"setNo\":101"),
                Arguments.of("\"setNo\":1", "\"setNo\":null"),
                Arguments.of("\"reps\":8", "\"reps\":1001"),
                Arguments.of("\"reps\":8", "\"reps\":null"),
                Arguments.of("\"weight\":42.75", "\"weight\":-0.01"),
                Arguments.of("\"weight\":42.75", "\"weight\":2000.01"),
                Arguments.of("\"weight\":42.75", "\"weight\":1.001"),
                Arguments.of("\"weight\":42.75", "\"weight\":null"),
                Arguments.of("\"sets\":[", "\"sets\":[null],\"ignored\":["),
                Arguments.of("\"sets\":[", "\"sets\":[],\"ignored\":["),
                Arguments.of("\"sets\":[", "\"sets\":null,\"ignored\":["),
                Arguments.of("\"time\":\"" + WHEN + "\"", "\"time\":null"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "0", "-1"})
    void validatesResultIdentifiers(String value) throws Exception {
        String token = token("owner@progress.example", UserRole.USER);
        Plan plan = plan(token);
        for (String field : new String[] {"planId", "planDayId", "exerciseId"}) {
            String body = payload(plan).replaceAll("\"" + field + "\":\\d+", "\"" + field + "\":" + value);
            mvc.perform(auth(post("/api/results"), token).content(body)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void rejectsMoreThanFiveHundredSets() throws Exception {
        String token = token("owner@progress.example", UserRole.USER);
        Plan plan = plan(token);
        String body = "{\"time\":\"" + WHEN + "\",\"planId\":" + plan.id() + ",\"planDayId\":" + plan.dayId()
                + ",\"sets\":[" + String.join(",", Collections.nCopies(501, set(1, 8, "0"))) + "]}";
        mvc.perform(auth(post("/api/results"), token).content(body)).andExpect(status().isBadRequest());
        assertThat(count("exercise_results")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"weightKg\":0", "\"weightKg\":500.01", "\"weightKg\":75.123", "\"chestCm\":301",
            "\"waistCm\":9", "\"hipsCm\":0", "\"armCm\":151", "\"thighCm\":201", ""})
    void validatesBodyMeasurementsAndRequiresAtLeastOne(String field) throws Exception {
        String token = token("owner@progress.example", UserRole.USER);
        String body = "{\"time\":\"" + WHEN + "\"" + (field.isEmpty() ? "" : "," + field) + "}";
        mvc.perform(auth(post("/api/body-metrics"), token).content(body)).andExpect(status().isBadRequest());
        assertThat(count("body_metrics")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1999-12-31T23:59:59Z", "future"})
    void rejectsPastBoundaryAndFutureDates(String when) throws Exception {
        String token = token("owner@progress.example", UserRole.USER);
        if (when.equals("future")) when = Instant.now().plusSeconds(3600).toString();
        mvc.perform(auth(post("/api/results"), token).content(payload(plan(token)).replace(WHEN, when)))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(post("/api/body-metrics"), token).content(metric().replace(WHEN, when)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidFiltersAndMalformedIdentifiersReturnBadRequest() throws Exception {
        String token = token("owner@progress.example", UserRole.USER);
        for (String endpoint : new String[] {"/api/results", "/api/body-metrics"}) {
            mvc.perform(auth(get(endpoint), token).param("from", "not-a-date")).andExpect(status().isBadRequest());
            mvc.perform(auth(get(endpoint), token).param("from", "2026-02-01T00:00:00Z").param("to", WHEN))
                    .andExpect(status().isBadRequest());
            mvc.perform(auth(get(endpoint), token).param("from", "+10000-01-01T00:00:00Z"))
                    .andExpect(status().isBadRequest());
            mvc.perform(auth(delete(endpoint + "/invalid"), token).param("time", WHEN)).andExpect(status().isBadRequest());
            mvc.perform(auth(delete(endpoint + "/" + UUID.randomUUID()), token).param("time", "invalid"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(auth(get("/api/results"), token).param("exerciseId", "-1")).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/results"), token).param("exerciseId", "wrong")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid", "disabled", "revoked"})
    void everyOperationRequiresAnActiveSession(String state) throws Exception {
        String token = state.equals("missing") ? null : state.equals("invalid") ? "invalid" : token("owner@progress.example", UserRole.USER);
        if (state.equals("disabled")) jdbc.update("update users set enabled = false");
        if (state.equals("revoked")) jdbc.update("update users set token_version = token_version + 1");
        for (var request : new MockHttpServletRequestBuilder[] {get("/api/results"), get("/api/body-metrics"),
                post("/api/results").content("{}"), post("/api/body-metrics").content("{}"),
                delete("/api/results/" + UUID.randomUUID()).param("time", WHEN),
                delete("/api/body-metrics/" + UUID.randomUUID()).param("time", WHEN)}) {
            mvc.perform(auth(request, token)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void confirmedAccountDeletionCascadesOwnProgressAndPreservesOtherUsersAndCatalog() throws Exception {
        String owner = token("owner@progress.example", UserRole.USER);
        String other = token("other@progress.example", UserRole.USER);
        for (String token : new String[] {owner, other}) {
            mvc.perform(auth(post("/api/results"), token).content(payload(plan(token)))).andExpect(status().isCreated());
            mvc.perform(auth(post("/api/body-metrics"), token).content(metric())).andExpect(status().isCreated());
        }
        String raw = "a".repeat(43);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        jdbc.update("insert into verification_tokens (user_id,token,type,expires_at) values (?,?,'ACCOUNT_DELETION',?)",
                userId("owner@progress.example"), digest, Timestamp.from(Instant.now().plusSeconds(600)));
        mvc.perform(post("/api/auth/confirm-deletion").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + raw + "\"}"))
                .andExpect(status().isOk());
        assertThat(count("exercise_results")).isEqualTo(1);
        assertThat(count("body_metrics")).isEqualTo(1);
        assertThat(count("exercises")).isEqualTo(48);
        mvc.perform(auth(get("/api/results"), owner)).andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/results"), other)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void historyCapsAreDeterministicAndDateFilterCanRetrieveOlderRows() throws Exception {
        String token = token("owner@progress.example", UserRole.USER);
        long ownerId = userId("owner@progress.example");
        Instant base = Instant.parse("2025-01-01T00:00:00Z");
        jdbc.batchUpdate("insert into exercise_results (id,time,user_id,exercise_id,exercise_name,plan_name,day_name,set_no,reps,weight)"
                + " values (?,?,?,?,?,?,?,1,8,0)", IntStream.range(0, 2001).boxed().toList(), 100, (statement, index) -> {
                    statement.setObject(1, UUID.randomUUID());
                    statement.setTimestamp(2, Timestamp.from(base.plusSeconds(index)));
                    statement.setLong(3, ownerId);
                    statement.setLong(4, exerciseId);
                    statement.setString(5, "Exercise");
                    statement.setString(6, "Plan");
                    statement.setString(7, "Day");
                });
        jdbc.batchUpdate("insert into body_metrics (id,time,user_id,weight_kg) values (?,?,?,80)",
                IntStream.range(0, 501).boxed().toList(), 100, (statement, index) -> {
                    statement.setObject(1, UUID.randomUUID());
                    statement.setTimestamp(2, Timestamp.from(base.plusSeconds(index)));
                    statement.setLong(3, ownerId);
                });
        mvc.perform(auth(get("/api/results"), token)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2000)))
                .andExpect(jsonPath("$[0].time").value(base.plusSeconds(2000).toString()));
        mvc.perform(auth(get("/api/body-metrics"), token)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(500)))
                .andExpect(jsonPath("$[0].time").value(base.plusSeconds(500).toString()));
        for (String endpoint : new String[] {"/api/results", "/api/body-metrics"}) {
            mvc.perform(auth(get(endpoint), token).param("to", base.toString())).andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].time").value(base.toString()));
        }
    }

    private Plan plan(String token) throws Exception {
        String body = "{\"name\":\"Plan progresu\",\"days\":[{\"name\":\"Pierwszy dzień\",\"exercises\":["
                + "{\"exerciseId\":" + exerciseId + ",\"sets\":3,\"reps\":8,\"weight\":42.75}]}]}";
        String response = mvc.perform(auth(post("/api/plans"), token).content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Plan(((Number) JsonPath.read(response, "$.id")).longValue(),
                ((Number) JsonPath.read(response, "$.days[0].id")).longValue());
    }

    private String payload(Plan plan) {
        return "{\"time\":\"" + WHEN + "\",\"planId\":" + plan.id() + ",\"planDayId\":" + plan.dayId()
                + ",\"sets\":[" + set(1, 8, "42.75") + "]}";
    }

    private String set(int setNo, int reps, String weight) {
        return "{\"exerciseId\":" + exerciseId + ",\"setNo\":" + setNo + ",\"reps\":" + reps + ",\"weight\":" + weight + "}";
    }

    private String metric() { return "{\"time\":\"" + WHEN + "\",\"weightKg\":82.25,\"waistCm\":90.5}"; }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        if (token != null) request.header("Authorization", "Bearer " + token);
        return request.contentType(MediaType.APPLICATION_JSON);
    }

    private String token(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName("Progress API test");
        user.setPasswordHash("unused-in-api-tests");
        user.setRole(role);
        return jwtService.generateToken(users.saveAndFlush(user));
    }

    private long userId(String email) { return users.findByEmailIgnoreCase(email).orElseThrow().getId(); }
    private long count(String table) { return jdbc.queryForObject("select count(*) from " + table, Long.class); }
    private record Plan(long id, long dayId) { }
}
