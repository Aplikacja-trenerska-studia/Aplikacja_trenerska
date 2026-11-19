package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.jayway.jsonpath.JsonPath;
import com.studia.aplikacja_trenerska.auth.service.JwtService;
import com.studia.aplikacja_trenerska.user.entity.*;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrainerApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean JavaMailSender mail;
    private String trainer;
    private String trainee;
    private Long traineeId;
    private Long exerciseId;

    @BeforeEach void prepare() {
        cleanup();
        trainer = token("trainer@trainer.example", UserRole.TRAINER);
        trainee = token("trainee@trainer.example", UserRole.USER);
        traineeId = user("trainee@trainer.example").getId();
        exerciseId = jdbc.queryForList("select id from exercises order by id", Long.class).getFirst();
    }
    @AfterEach void cleanup() { jdbc.update("delete from users"); }

    @Test void groupCrudAndMembershipConsent() throws Exception {
        long groupId = group(trainer);
        mvc.perform(auth(get("/api/groups"), trainer)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        invite(groupId);
        // Inviting the same person twice is safe and does not duplicate membership.
        invite(groupId);
        mvc.perform(auth(get("/api/groups/{id}", groupId), trainer)).andExpect(status().isOk())
                .andExpect(jsonPath("members", hasSize(1))).andExpect(jsonPath("members[0].status").value("PENDING"));
        mvc.perform(auth(get("/api/my-invitations"), trainee)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(groupId)).andExpect(jsonPath("$[0].status").value("PENDING"));
        accept(groupId);
        mvc.perform(auth(get("/api/groups/{id}", groupId), trainer)).andExpect(status().isOk())
                .andExpect(jsonPath("members[0].status").value("ACCEPTED")).andExpect(jsonPath("members[0].acceptedAt").isString());
        mvc.perform(auth(delete("/api/groups/{id}", groupId), trainer)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/groups/{id}", groupId), trainer)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/my-invitations"), trainee)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test void assignmentCreatesIndependentOwnedCopyAndIsIdempotent() throws Exception {
        long groupId = acceptedGroup();
        long source = plan(trainer, "Original");
        String assigned = assign(groupId, source, traineeId);
        long copy = ((Number) JsonPath.read(assigned, "$[0].planId")).longValue();
        assertThat(copy).isNotEqualTo(source);
        assertThat(jdbc.queryForObject("select owner_id from workout_plans where id = ?", Long.class, copy)).isEqualTo(traineeId);
        assertThat(jdbc.queryForObject("select count(*) from plan_days", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from plan_exercises", Integer.class)).isEqualTo(2);
        assertThat(assign(groupId, source, traineeId)).isEqualTo(assigned);
        mvc.perform(auth(get("/api/my-assignments"), trainee)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].planId").value(copy));
        mvc.perform(auth(get("/api/my-assignments"), trainer)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(auth(get("/api/plans/{id}", copy), trainee)).andExpect(status().isOk()).andExpect(jsonPath("name").value("Original"));
        mvc.perform(auth(get("/api/plans/{id}", copy), trainer)).andExpect(status().isNotFound());
        mvc.perform(auth(put("/api/plans/{id}", source), trainer).content(planBody("Changed"))).andExpect(status().isOk());
        mvc.perform(auth(get("/api/plans/{id}", copy), trainee)).andExpect(status().isOk()).andExpect(jsonPath("name").value("Original"));
        mvc.perform(auth(delete("/api/plans/{id}", source), trainer)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/my-assignments"), trainee)).andExpect(status().isOk()).andExpect(jsonPath("$[0].planId").value(copy));
        mvc.perform(auth(delete("/api/groups/{id}", groupId), trainer)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/plans/{id}", copy), trainee)).andExpect(status().isOk());
    }

    @Test void onlyAcceptedMembersCanReceivePlanAndWholeGroupSkipsPending() throws Exception {
        long groupId = group(trainer);
        invite(groupId);
        long source = plan(trainer, "Source");
        mvc.perform(auth(post("/api/groups/{id}/assign-plan", groupId), trainer)
                .content("{\"planId\":" + source + ",\"userId\":" + traineeId + "}"))
                .andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/groups/{id}/assign-plan", groupId), trainer).content("{\"planId\":" + source + "}"))
                .andExpect(status().isConflict());
        accept(groupId);
        token("pending@trainer.example", UserRole.USER);
        mvc.perform(auth(post("/api/groups/{id}/members", groupId), trainer).content("{\"email\":\"pending@trainer.example\"}"))
                .andExpect(status().isOk());
        mvc.perform(auth(post("/api/groups/{id}/assign-plan", groupId), trainer).content("{\"planId\":" + source + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].userId").value(traineeId));
    }

    @Test void pendingDeclinedAndRemovedMembershipsCannotReadProgress() throws Exception {
        long groupId = group(trainer);
        invite(groupId);
        progressRequest(groupId, traineeId, "results", 404);
        progressRequest(groupId, traineeId, "body-metrics", 404);
        accept(groupId);
        progressRequest(groupId, traineeId, "results", 200);
        progressRequest(groupId, traineeId, "body-metrics", 200);
        mvc.perform(auth(delete("/api/my-invitations/{id}", groupId), trainee)).andExpect(status().isNoContent());
        progressRequest(groupId, traineeId, "results", 404);
        invite(groupId);
        accept(groupId);
        mvc.perform(auth(delete("/api/groups/{group}/members/{user}", groupId, traineeId), trainer)).andExpect(status().isNoContent());
        progressRequest(groupId, traineeId, "body-metrics", 404);
    }

    @Test void trainerReadsAcceptedMembersActualProgressButCannotReadStrangers() throws Exception {
        long groupId = acceptedGroup();
        mvc.perform(auth(post("/api/body-metrics"), trainee).content("{\"time\":\"2026-11-18T12:00:00Z\",\"weightKg\":72.4}"))
                .andExpect(status().isCreated());
        mvc.perform(auth(get("/api/groups/{group}/members/{user}/body-metrics", groupId, traineeId), trainer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].weightKg").value(72.4));
        token("stranger@trainer.example", UserRole.USER);
        progressRequest(groupId, user("stranger@trainer.example").getId(), "body-metrics", 404);
        mvc.perform(auth(get("/api/groups/{group}/members/{user}/body-metrics", groupId, traineeId), trainer)
                .param("from", "2026-11-19T00:00:00Z")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test void foreignTrainerCannotAccessGroupMembersAssignmentsOrProgress() throws Exception {
        long groupId = acceptedGroup();
        String other = token("other@trainer.example", UserRole.TRAINER);
        long source = plan(other, "Other plan");
        mvc.perform(auth(get("/api/groups/{id}", groupId), other)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/groups/{id}", groupId), other)).andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/groups/{id}/members", groupId), other).content("{\"email\":\"trainee@trainer.example\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/groups/{id}/assign-plan", groupId), other).content("{\"planId\":" + source + "}"))
                .andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/groups/{group}/members/{user}/results", groupId, traineeId), other))
                .andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/groups/{id}/assign-plan", groupId), trainer).content("{\"planId\":" + source + "}"))
                .andExpect(status().isNotFound());
    }

    @Test void allTrainerRoutesRequireTrainerAndActiveAuthentication() throws Exception {
        long groupId = acceptedGroup();
        for (var request : new MockHttpServletRequestBuilder[] {get("/api/groups"), get("/api/groups/" + groupId),
                post("/api/groups").content("{\"name\":\"G\"}"), delete("/api/groups/" + groupId),
                get("/api/groups/" + groupId + "/members/" + traineeId + "/results")}) {
            mvc.perform(auth(request, trainee)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/groups")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/my-invitations")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/my-assignments")).andExpect(status().isUnauthorized());
        jdbc.update("update users set role = 'USER', token_version = token_version + 1 where email = 'trainer@trainer.example'");
        mvc.perform(auth(get("/api/groups"), trainer)).andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/my-invitations"), trainee)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(auth(post("/api/my-invitations/{id}/accept", groupId), trainee)).andExpect(status().isNotFound());
    }

    @Test void validationAndInvitationOwnership() throws Exception {
        mvc.perform(auth(post("/api/groups"), trainer).content("{\"name\":\"  \"}")).andExpect(status().isBadRequest());
        long groupId = group(trainer);
        mvc.perform(auth(post("/api/groups/{id}/members", groupId), trainer).content("{\"email\":\"broken\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(post("/api/groups/{id}/members", groupId), trainer).content("{\"email\":\"trainer@trainer.example\"}"))
                .andExpect(status().isNotFound());
        invite(groupId);
        String stranger = token("stranger@trainer.example", UserRole.USER);
        mvc.perform(auth(post("/api/my-invitations/{id}/accept", groupId), stranger)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/my-invitations/{id}", groupId), stranger)).andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/groups/{id}/assign-plan", groupId), trainer).content("{\"planId\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test void deletingTrainerCascadesRelationsAndPreservesRecipientCopy() throws Exception {
        long groupId = acceptedGroup();
        long source = plan(trainer, "Source");
        long copy = ((Number) JsonPath.read(assign(groupId, source, traineeId), "$[0].planId")).longValue();
        jdbc.update("delete from users where email = 'trainer@trainer.example'");
        assertThat(jdbc.queryForObject("select count(*) from training_groups", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from group_members", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from plan_assignments", Integer.class)).isZero();
        mvc.perform(auth(get("/api/plans/{id}", copy), trainee)).andExpect(status().isOk());
        jdbc.update("delete from users where id = ?", traineeId);
        assertThat(jdbc.queryForObject("select count(*) from workout_plans", Integer.class)).isZero();
    }

    @Test void deletingRecipientCopyAllowsExplicitReassignmentAndLeavingPreservesOwnedPlan() throws Exception {
        long groupId = acceptedGroup();
        long source = plan(trainer, "Source");
        long first = ((Number) JsonPath.read(assign(groupId, source, traineeId), "$[0].planId")).longValue();
        mvc.perform(auth(delete("/api/plans/{id}", first), trainee)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/my-assignments"), trainee)).andExpect(status().isOk()).andExpect(jsonPath("$[0].planId").isEmpty());
        long second = ((Number) JsonPath.read(assign(groupId, source, traineeId), "$[0].planId")).longValue();
        assertThat(second).isNotEqualTo(first);
        mvc.perform(auth(delete("/api/my-invitations/{id}", groupId), trainee)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/my-assignments"), trainee)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(auth(get("/api/plans/{id}", second), trainee)).andExpect(status().isOk());
    }

    @Test
    @Timeout(15)
    void acceptingAnInvitationDuringConcurrentRemovalReturns404WithoutStaleRowFailure() throws Exception {
        long groupId = group(trainer);
        invite(groupId);
        var locked = new CountDownLatch(1);
        var removeNow = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var removal = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                jdbc.queryForList("select user_id from group_members where group_id = ? and user_id = ? for update", groupId, traineeId);
                locked.countDown();
                try {
                    if (!removeNow.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Removal latch timed out");
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                jdbc.update("delete from group_members where group_id = ? and user_id = ?", groupId, traineeId);
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            var acceptance = executor.submit(() -> mvc.perform(auth(post("/api/my-invitations/{id}/accept", groupId), trainee))
                    .andReturn().getResponse().getStatus());
            try {
                // Let the request encounter the locked row before its concurrent removal commits.
                Thread.sleep(100);
            } finally { removeNow.countDown(); }
            removal.get(5, TimeUnit.SECONDS);
            assertThat(acceptance.get(5, TimeUnit.SECONDS)).isEqualTo(404);
        } finally { removeNow.countDown(); }
        assertThat(jdbc.queryForObject("select count(*) from group_members", Integer.class)).isZero();
    }

    private long acceptedGroup() throws Exception { long groupId = group(trainer); invite(groupId); accept(groupId); return groupId; }
    private long group(String token) throws Exception {
        String body = mvc.perform(auth(post("/api/groups"), token).content("{\"name\":\" Test group \"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("name").value("Test group"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
    private void invite(long groupId) throws Exception {
        mvc.perform(auth(post("/api/groups/{id}/members", groupId), trainer).content("{\"email\":\"trainee@trainer.example\"}"))
                .andExpect(status().isOk());
    }
    private void accept(long groupId) throws Exception {
        mvc.perform(auth(post("/api/my-invitations/{id}/accept", groupId), trainee)).andExpect(status().isNoContent());
    }
    private String assign(long groupId, long planId, Long userId) throws Exception {
        return mvc.perform(auth(post("/api/groups/{id}/assign-plan", groupId), trainer)
                .content("{\"planId\":" + planId + ",\"userId\":" + userId + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
    private void progressRequest(long groupId, Long userId, String resource, int expected) throws Exception {
        mvc.perform(auth(get("/api/groups/{group}/members/{user}/" + resource, groupId, userId), trainer))
                .andExpect(status().is(expected));
    }
    private long plan(String token, String name) throws Exception {
        String body = mvc.perform(auth(post("/api/plans"), token).content(planBody(name))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
    private String planBody(String name) {
        return "{\"name\":\"" + name + "\",\"days\":[{\"name\":\"Day\",\"exercises\":[{\"exerciseId\":"
                + exerciseId + ",\"sets\":3,\"reps\":10,\"weight\":40}]}]}";
    }
    private User user(String email) { return users.findByEmailIgnoreCase(email).orElseThrow(); }
    private String token(String email, UserRole role) {
        User user = new User(); user.setEmail(email); user.setDisplayName(email.split("@")[0]);
        user.setRole(role); user.setPasswordHash("unused-test-password");
        return jwt.generateToken(users.saveAndFlush(user));
    }
    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }
}
