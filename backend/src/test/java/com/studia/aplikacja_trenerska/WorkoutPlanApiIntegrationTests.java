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

import java.util.Collections;
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

// No test transaction: HTTP calls must commit/rollback independently, as in the running application.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkoutPlanApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired JwtService jwtService;
    @MockitoBean JavaMailSender mailSender;
    private Long firstExercise;
    private Long secondExercise;

    @BeforeEach
    void prepare() {
        cleanup();
        var ids = jdbc.queryForList("select id from exercises order by id", Long.class);
        firstExercise = ids.get(0);
        secondExercise = ids.get(1);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("delete from users");
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void fullCrudRoundTripPreservesOrderAndRemovesReplacedChildren(UserRole role) throws Exception {
        String token = token("owner@plans.example", role);
        mvc.perform(auth(get("/api/plans"), token)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        String body = """
                {"name":" Plan A ","description":" Notes ","days":[
                  {"name":" Day A ","exercises":[
                    {"exerciseId":%d,"sets":3,"reps":10,"weight":42.75},
                    {"exerciseId":%d,"sets":2,"reps":8,"weight":0}]},
                  {"name":"Day B","exercises":[{"exerciseId":%d,"sets":1,"reps":12,"weight":0}]}]}
                """.formatted(firstExercise, secondExercise, firstExercise);
        String created = mvc.perform(auth(post("/api/plans"), token).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("name").value("Plan A"))
                .andExpect(jsonPath("description").value("Notes"))
                .andExpect(jsonPath("days", hasSize(2))).andExpect(jsonPath("days[0].name").value("Day A"))
                .andExpect(jsonPath("days[0].exercises[0].weight").value(42.75))
                .andExpect(jsonPath("owner").doesNotExist()).andReturn().getResponse().getContentAsString();
        long id = id(created);
        mvc.perform(auth(get("/api/plans/{id}", id), token)).andExpect(status().isOk())
                .andExpect(jsonPath("days[1].dayIndex").value(1))
                .andExpect(jsonPath("days[0].exercises[1].orderIndex").value(1))
                .andExpect(jsonPath("days[0].exercises[1].exercise.id").value(secondExercise))
                .andExpect(jsonPath("days[0].exercises[1].weight").value(0));
        mvc.perform(auth(get("/api/plans"), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].days").doesNotExist());

        String updated = """
                {"name":"Updated","days":[{"name":"Replacement","exercises":[
                  {"exerciseId":%d,"sets":4,"reps":6,"weight":27.50},
                  {"exerciseId":%d,"sets":1,"reps":20,"weight":0}]}]}
                """.formatted(secondExercise, firstExercise);
        // Repeat a full replacement to exercise uniqueness of positions on successive PUTs.
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(auth(put("/api/plans/{id}", id), token).content(updated)).andExpect(status().isOk())
                    .andExpect(jsonPath("id").value(id)).andExpect(jsonPath("name").value("Updated"))
                    .andExpect(jsonPath("days", hasSize(1)))
                    .andExpect(jsonPath("days[0].exercises[0].exercise.id").value(secondExercise))
                    .andExpect(jsonPath("days[0].exercises[0].sets").value(4));
        }
        assertThat(jdbc.queryForObject("select count(*) from plan_days", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from plan_exercises", Integer.class)).isEqualTo(2);
        mvc.perform(auth(delete("/api/plans/{id}", id), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/plans/{id}", id), token)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/plans/{id}", id), token)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from plan_days", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from plan_exercises", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from exercises", Integer.class)).isEqualTo(48);
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void otherAccountsCannotReadChangeOrDeleteAPlanEvenWhenSharedFlagIsSet(UserRole callerRole) throws Exception {
        String owner = token("owner@plans.example", UserRole.USER);
        long foreignId = create(owner, minimalPayload());
        jdbc.update("update workout_plans set is_shared = true where id = ?", foreignId);
        String caller = token("caller@plans.example", callerRole);
        long ownId = create(caller, minimalPayload());
        mvc.perform(auth(get("/api/plans"), caller)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(ownId));
        String foreignError = mvc.perform(auth(get("/api/plans/{id}", foreignId), caller))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String missingError = mvc.perform(auth(get("/api/plans/{id}", Long.MAX_VALUE), caller))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(foreignError).isEqualTo(missingError);
        mvc.perform(auth(put("/api/plans/{id}", foreignId), caller).content(minimalPayload()))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/plans/{id}", foreignId), caller)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/plans/{id}", foreignId), owner)).andExpect(status().isOk());
    }

    @Test
    void cannotChooseOwnerOrEnableSharingThroughRequestBody() throws Exception {
        String owner = token("owner@plans.example", UserRole.USER);
        token("other@plans.example", UserRole.TRAINER);
        Long otherId = users.findByEmailIgnoreCase("other@plans.example").orElseThrow().getId();
        String payload = "{\"ownerId\":" + otherId + ",\"isShared\":true," + minimalPayload().substring(1);
        long id = create(owner, payload);
        mvc.perform(auth(put("/api/plans/{id}", id), owner).content(payload)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select owner_id from workout_plans where id = ?", Long.class, id))
                .isEqualTo(users.findByEmailIgnoreCase("owner@plans.example").orElseThrow().getId());
        assertThat(jdbc.queryForObject("select is_shared from workout_plans where id = ?", Boolean.class, id)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid", "disabled", "revoked"})
    void everyOperationRequiresAnActiveSession(String state) throws Exception {
        String token = state.equals("missing") ? null : state.equals("invalid") ? "invalid" : token("owner@plans.example", UserRole.USER);
        if (state.equals("disabled")) jdbc.update("update users set enabled = false");
        if (state.equals("revoked")) jdbc.update("update users set token_version = token_version + 1");
        for (var request : new MockHttpServletRequestBuilder[] {get("/api/plans"), get("/api/plans/1"),
                post("/api/plans").content(minimalPayload()), put("/api/plans/1").content(minimalPayload()), delete("/api/plans/1")}) {
            mvc.perform(auth(request, token)).andExpect(status().isUnauthorized());
        }
    }

    @ParameterizedTest
    @MethodSource("invalidPayloadChanges")
    void validatesNestedPayloadWithoutCreatingAnything(String original, String replacement) throws Exception {
        String token = token("owner@plans.example", UserRole.USER);
        mvc.perform(auth(post("/api/plans"), token).content(minimalPayload().replace(original, replacement)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").isString())
                .andExpect(jsonPath("errors").isMap());
        assertThat(jdbc.queryForObject("select count(*) from workout_plans", Integer.class)).isZero();
    }

    static Stream<Arguments> invalidPayloadChanges() {
        return Stream.of(
                Arguments.of("\"name\":\"Plan A\"", "\"name\":\"   \""),
                Arguments.of("\"name\":\"Plan A\"", "\"name\":\"" + "x".repeat(121) + "\""),
                Arguments.of("\"description\":\"Notes\"", "\"description\":\"" + "x".repeat(2001) + "\""),
                Arguments.of("\"days\":[", "\"days\":[],\"ignored\":["),
                Arguments.of("\"days\":[", "\"days\":null,\"ignored\":["),
                Arguments.of("\"days\":[", "\"days\":[null],\"ignored\":["),
                Arguments.of("\"name\":\"Day A\"", "\"name\":\" \""),
                Arguments.of("\"name\":\"Day A\"", "\"name\":\"" + "x".repeat(121) + "\""),
                Arguments.of("\"exercises\":[", "\"exercises\":[],\"ignored\":["),
                Arguments.of("\"exercises\":[", "\"exercises\":null,\"ignored\":["),
                Arguments.of("\"exercises\":[", "\"exercises\":[null],\"ignored\":["),
                Arguments.of("\"sets\":3", "\"sets\":0"),
                Arguments.of("\"sets\":3", "\"sets\":null"),
                Arguments.of("\"reps\":10", "\"reps\":0"),
                Arguments.of("\"reps\":10", "\"reps\":null"),
                Arguments.of("\"weight\":0", "\"weight\":null"),
                Arguments.of("\"weight\":0", "\"weight\":-0.01"),
                Arguments.of("\"weight\":0", "\"weight\":1.001"),
                Arguments.of("\"weight\":0", "\"weight\":100000"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"days", "exercises"})
    void rejectsOversizedCollections(String collection) throws Exception {
        String entry = "{\"exerciseId\":" + firstExercise + ",\"sets\":3,\"reps\":10,\"weight\":0}";
        String day = "{\"name\":\"Day\",\"exercises\":[" + entry + "]}";
        String payload = collection.equals("days")
                ? "{\"name\":\"Plan\",\"days\":[" + String.join(",", Collections.nCopies(32, day)) + "]}"
                : "{\"name\":\"Plan\",\"days\":[{\"name\":\"Day\",\"exercises\":[" + String.join(",", Collections.nCopies(101, entry)) + "]}]}";
        mvc.perform(auth(post("/api/plans"), token("owner@plans.example", UserRole.USER)).content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownExerciseAndInvalidUpdateLeaveExistingPlanUnchanged() throws Exception {
        String token = token("owner@plans.example", UserRole.USER);
        long id = create(token, minimalPayload());
        String before = mvc.perform(auth(get("/api/plans/{id}", id), token)).andReturn().getResponse().getContentAsString();
        String unknown = minimalPayload().replace("\"exerciseId\":" + firstExercise, "\"exerciseId\":" + Long.MAX_VALUE);
        mvc.perform(auth(post("/api/plans"), token).content(unknown)).andExpect(status().isBadRequest());
        mvc.perform(auth(put("/api/plans/{id}", id), token).content(unknown)).andExpect(status().isBadRequest());
        mvc.perform(auth(put("/api/plans/{id}", id), token).content(minimalPayload().replace("\"sets\":3", "\"sets\":-1")))
                .andExpect(status().isBadRequest());
        String after = mvc.perform(auth(get("/api/plans/{id}", id), token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(after).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from workout_plans", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "0", "-1"})
    void exerciseIdsMustBePresentAndPositive(String exerciseId) throws Exception {
        String payload = minimalPayload().replace("\"exerciseId\":" + firstExercise, "\"exerciseId\":" + exerciseId);
        mvc.perform(auth(post("/api/plans"), token("owner@plans.example", UserRole.USER)).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors['days[0].exercises[0].exerciseId']").isString());
        assertThat(jdbc.queryForObject("select count(*) from workout_plans", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "null"})
    void malformedOrMissingJsonReturnsStructuredError(String payload) throws Exception {
        mvc.perform(auth(post("/api/plans"), token("owner@plans.example", UserRole.USER)).content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").isString());
    }

    @Test
    void missingAndMalformedPlanIdsAreHandled() throws Exception {
        String token = token("owner@plans.example", UserRole.USER);
        mvc.perform(auth(get("/api/plans/not-a-number"), token)).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/plans/-1"), token)).andExpect(status().isNotFound());
        mvc.perform(auth(put("/api/plans/{id}", Long.MAX_VALUE), token).content(minimalPayload())).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/plans/{id}", Long.MAX_VALUE), token)).andExpect(status().isNotFound());
    }

    @Test
    void listIsNewestFirstWithIdAsTieBreakerAndDuplicateExercisesAreAllowed() throws Exception {
        String token = token("owner@plans.example", UserRole.USER);
        long first = create(token, minimalPayload());
        long second = create(token, minimalPayload());
        jdbc.update("update workout_plans set created_at = '2026-11-15T12:00:00Z'");
        mvc.perform(auth(get("/api/plans"), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(second)).andExpect(jsonPath("$[1].id").value(first));
        String duplicate = "{\"name\":\"Repeated\",\"days\":[{\"name\":\"Day\",\"exercises\":["
                + "{\"exerciseId\":" + firstExercise + ",\"sets\":3,\"reps\":10,\"weight\":0},"
                + "{\"exerciseId\":" + firstExercise + ",\"sets\":2,\"reps\":8,\"weight\":0}]}]}";
        mvc.perform(auth(post("/api/plans"), token).content(duplicate)).andExpect(status().isCreated())
                .andExpect(jsonPath("days[0].exercises", hasSize(2)));
    }

    private String minimalPayload() {
        return "{\"name\":\"Plan A\",\"description\":\"Notes\",\"days\":[{\"name\":\"Day A\",\"exercises\":["
                + "{\"exerciseId\":" + firstExercise + ",\"sets\":3,\"reps\":10,\"weight\":0}]}]}";
    }

    private long create(String token, String payload) throws Exception {
        var result = mvc.perform(auth(post("/api/plans"), token).content(payload)).andExpect(status().isCreated());
        String response = result.andReturn().getResponse().getContentAsString();
        long id = id(response);
        result.andExpect(header().string("Location", "/api/plans/" + id));
        return id;
    }

    private long id(String response) {
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        if (token != null) request.header("Authorization", "Bearer " + token);
        return request.contentType(MediaType.APPLICATION_JSON);
    }

    private String token(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName("Plan API test");
        user.setPasswordHash("unused-in-api-tests");
        user.setRole(role);
        return jwtService.generateToken(users.saveAndFlush(user));
    }
}
