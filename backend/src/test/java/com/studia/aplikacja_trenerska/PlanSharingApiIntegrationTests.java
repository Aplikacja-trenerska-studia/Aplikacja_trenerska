package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
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
class PlanSharingApiIntegrationTests {
    private static final String TOKEN_PATTERN = "[A-Za-z0-9_-]{43}";
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
    void ownerSharesPlanAndAnotherAccountViewsAndCopiesIt(UserRole viewerRole) throws Exception {
        String owner = token("owner@share.example", UserRole.USER);
        String viewer = token("viewer@share.example", viewerRole);
        long planId = plan(owner);
        String shareToken = share(owner, planId);
        mvc.perform(auth(get("/api/plans/{id}", planId), owner)).andExpect(status().isOk())
                .andExpect(jsonPath("shared").value(true)).andExpect(jsonPath("shareToken").value(shareToken));
        mvc.perform(auth(get("/api/plans"), owner)).andExpect(status().isOk()).andExpect(jsonPath("$[0].shared").value(true))
                .andExpect(jsonPath("$[0].shareToken").doesNotExist());
        mvc.perform(auth(get("/api/shared-plans/{token}", shareToken), viewer)).andExpect(status().isOk())
                .andExpect(jsonPath("name").value("Plan do udostępnienia"))
                .andExpect(jsonPath("ownerName").value("Share test"))
                .andExpect(jsonPath("own").value(false))
                .andExpect(jsonPath("days", hasSize(1)))
                .andExpect(jsonPath("days[0].name").value("Dzień góra"))
                .andExpect(jsonPath("days[0].exercises[0].exercise.id").value(exerciseId))
                .andExpect(jsonPath("days[0].exercises[0].sets").value(4))
                .andExpect(jsonPath("id").doesNotExist())
                .andExpect(jsonPath("shareToken").doesNotExist())
                .andExpect(jsonPath("ownerId").doesNotExist());
        mvc.perform(auth(get("/api/shared-plans/{token}", shareToken), owner)).andExpect(status().isOk())
                .andExpect(jsonPath("own").value(true));
        String copy = mvc.perform(auth(post("/api/shared-plans/{token}/copy", shareToken), viewer))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("name").value("Plan do udostępnienia"))
                .andExpect(jsonPath("shared").value(false)).andExpect(jsonPath("shareToken").isEmpty())
                .andExpect(jsonPath("days[0].exercises[0].reps").value(6))
                .andReturn().getResponse().getContentAsString();
        long copyId = ((Number) JsonPath.read(copy, "$.id")).longValue();
        assertThat(copyId).isNotEqualTo(planId);
        assertThat(jdbc.queryForObject("select owner_id from workout_plans where id = ?", Long.class, copyId))
                .isEqualTo(userId("viewer@share.example"));
        mvc.perform(auth(get("/api/plans/{id}", copyId), viewer)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/plans/{id}", planId), viewer)).andExpect(status().isNotFound());
    }

    @Test
    void copyIsIndependentFromLaterChangesAndDeletionOfTheSource() throws Exception {
        String owner = token("owner@share.example", UserRole.USER);
        String viewer = token("viewer@share.example", UserRole.USER);
        long planId = plan(owner);
        String shareToken = share(owner, planId);
        long copyId = ((Number) JsonPath.read(mvc.perform(auth(post("/api/shared-plans/{token}/copy", shareToken), viewer))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id")).longValue();
        mvc.perform(auth(put("/api/plans/{id}", planId), owner).content(body("Zmieniony plan", otherExerciseId, 2)))
                .andExpect(status().isOk()).andExpect(jsonPath("shared").value(true))
                .andExpect(jsonPath("shareToken").value(shareToken));
        mvc.perform(auth(get("/api/shared-plans/{token}", shareToken), viewer)).andExpect(status().isOk())
                .andExpect(jsonPath("name").value("Zmieniony plan"));
        mvc.perform(auth(get("/api/plans/{id}", copyId), viewer)).andExpect(status().isOk())
                .andExpect(jsonPath("name").value("Plan do udostępnienia"))
                .andExpect(jsonPath("days[0].exercises[0].exercise.id").value(exerciseId));
        mvc.perform(auth(delete("/api/plans/{id}", planId), owner)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/shared-plans/{token}", shareToken), viewer)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/plans/{id}", copyId), viewer)).andExpect(status().isOk());
        jdbc.update("delete from users where email = 'owner@share.example'");
        assertThat(jdbc.queryForObject("select count(*) from workout_plans where id = ?", Integer.class, copyId)).isEqualTo(1);
    }

    @Test
    void repeatedShareKeepsTheLinkAndUnsharingRevokesIt() throws Exception {
        String owner = token("owner@share.example", UserRole.USER);
        String viewer = token("viewer@share.example", UserRole.USER);
        long planId = plan(owner);
        String first = share(owner, planId);
        assertThat(share(owner, planId)).isEqualTo(first);
        mvc.perform(auth(delete("/api/plans/{id}/share", planId), owner)).andExpect(status().isNoContent());
        mvc.perform(auth(delete("/api/plans/{id}/share", planId), owner)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/plans/{id}", planId), owner)).andExpect(status().isOk())
                .andExpect(jsonPath("shared").value(false)).andExpect(jsonPath("shareToken").isEmpty());
        mvc.perform(auth(get("/api/shared-plans/{token}", first), viewer)).andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/shared-plans/{token}/copy", first), viewer)).andExpect(status().isNotFound());
        String second = share(owner, planId);
        assertThat(second).isNotEqualTo(first);
        mvc.perform(auth(get("/api/shared-plans/{token}", first), viewer)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/shared-plans/{token}", second), viewer)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from workout_plans", Integer.class)).isEqualTo(1);
    }

    @Test
    void onlyTheOwnerCanEnableOrDisableSharing() throws Exception {
        String owner = token("owner@share.example", UserRole.USER);
        String other = token("other@share.example", UserRole.TRAINER);
        long planId = plan(owner);
        mvc.perform(auth(post("/api/plans/{id}/share", planId), other)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select is_shared from workout_plans where id = ?", Boolean.class, planId)).isFalse();
        String shareToken = share(owner, planId);
        mvc.perform(auth(delete("/api/plans/{id}/share", planId), other)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/shared-plans/{token}", shareToken), other)).andExpect(status().isOk());
        mvc.perform(auth(post("/api/plans/{id}/share", Long.MAX_VALUE), owner)).andExpect(status().isNotFound());
    }

    @Test
    void sharedFlagWithoutMatchingTokenDoesNotExposeAnyPlan() throws Exception {
        String owner = token("owner@share.example", UserRole.USER);
        String viewer = token("viewer@share.example", UserRole.USER);
        long planId = plan(owner);
        jdbc.update("update workout_plans set is_shared = true where id = ?", planId);
        for (String candidate : new String[] {"a".repeat(43), "short", "a".repeat(44), "x".repeat(42) + "."}) {
            mvc.perform(auth(get("/api/shared-plans/{token}", candidate), viewer)).andExpect(status().isNotFound());
            mvc.perform(auth(post("/api/shared-plans/{token}/copy", candidate), viewer)).andExpect(status().isNotFound());
        }
        assertThat(jdbc.queryForObject("select count(*) from workout_plans", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"blocked", "disabled"})
    void inactiveOwnerHidesTheSharedPlan(String state) throws Exception {
        String owner = token("owner@share.example", UserRole.USER);
        String viewer = token("viewer@share.example", UserRole.USER);
        String shareToken = share(owner, plan(owner));
        jdbc.update("update users set " + (state.equals("blocked") ? "blocked = true" : "enabled = false")
                + " where email = 'owner@share.example'");
        mvc.perform(auth(get("/api/shared-plans/{token}", shareToken), viewer)).andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/shared-plans/{token}/copy", shareToken), viewer)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid", "disabled", "revoked"})
    void everySharingOperationRequiresAnActiveSession(String state) throws Exception {
        String owner = token("owner@share.example", UserRole.USER);
        long planId = plan(owner);
        String shareToken = share(owner, planId);
        String token = state.equals("missing") ? null : state.equals("invalid") ? "invalid" : token("viewer@share.example", UserRole.USER);
        if (state.equals("disabled")) jdbc.update("update users set enabled = false where email = 'viewer@share.example'");
        if (state.equals("revoked")) jdbc.update("update users set token_version = token_version + 1 where email = 'viewer@share.example'");
        for (var request : new MockHttpServletRequestBuilder[] {post("/api/plans/" + planId + "/share"),
                delete("/api/plans/" + planId + "/share"), get("/api/shared-plans/" + shareToken),
                post("/api/shared-plans/" + shareToken + "/copy")}) {
            mvc.perform(auth(request, token)).andExpect(status().isUnauthorized());
        }
        assertThat(jdbc.queryForObject("select count(*) from workout_plans", Integer.class)).isEqualTo(1);
    }

    private String share(String token, long planId) throws Exception {
        String response = mvc.perform(auth(post("/api/plans/{id}/share", planId), token)).andExpect(status().isOk())
                .andExpect(jsonPath("shared").value(true)).andExpect(jsonPath("shareToken").value(matchesPattern(TOKEN_PATTERN)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.shareToken");
    }

    private long plan(String token) throws Exception {
        String response = mvc.perform(auth(post("/api/plans"), token).content(body("Plan do udostępnienia", exerciseId, 4)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String body(String name, long exercise, int sets) {
        return "{\"name\":\"" + name + "\",\"description\":\"Opis\",\"days\":[{\"name\":\"Dzień góra\",\"exercises\":["
                + "{\"exerciseId\":" + exercise + ",\"sets\":" + sets + ",\"reps\":6,\"weight\":60}]}]}";
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        if (token != null) request.header("Authorization", "Bearer " + token);
        return request.contentType(MediaType.APPLICATION_JSON);
    }

    private String token(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName("Share test");
        user.setPasswordHash("unused-in-api-tests");
        user.setRole(role);
        return jwtService.generateToken(users.saveAndFlush(user));
    }

    private long userId(String email) { return users.findByEmailIgnoreCase(email).orElseThrow().getId(); }
}
