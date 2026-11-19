package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Timestamp;
import java.time.Instant;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.studia.aplikacja_trenerska.auth.service.JwtService;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

// HTTP requests commit independently; no enclosing test transaction hides persisted state.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminApiIntegrationTests {
    private static final String PASSWORD = "AdminApiPassword123!";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder passwords;
    @MockitoBean JavaMailSender mail;
    private String admin;
    private long adminId;

    @BeforeEach void prepare() {
        cleanup();
        User owner = user("administrator@admin.example", "Administrator", UserRole.ADMIN, true, false);
        adminId = owner.getId();
        admin = jwt.generateToken(owner);
    }

    @AfterEach void cleanup() { jdbc.update("delete from users"); }

    @Test void anonymousRequestsCannotAccessAdministrativeDataOrChanges() throws Exception {
        for (String path : new String[] {"/api/admin", "/api/admin/overview", "/api/admin/users"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).header("Authorization", "Bearer invalid-token")).andExpect(status().isUnauthorized());
        }
        mvc.perform(patch("/api/admin/users/{id}/role", adminId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/users/{id}/block", adminId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"blocked\":true}")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"USER", "TRAINER"})
    void otherRolesCannotListUsersSeeOverviewOrChangeAccounts(UserRole role) throws Exception {
        String caller = jwt.generateToken(user("caller@admin.example", "Caller", role, true, false));
        mvc.perform(auth(get("/api/admin/overview"), caller)).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/admin/users"), caller)).andExpect(status().isForbidden());
        mvc.perform(auth(patch("/api/admin/users/{id}/role", adminId), caller).content("{\"role\":\"USER\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(auth(patch("/api/admin/users/{id}/block", adminId), caller).content("{\"blocked\":true}"))
                .andExpect(status().isForbidden());
        assertThat(users.findById(adminId).orElseThrow().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test void staleAdministratorClaimDoesNotOverrideCurrentDatabaseRoleOrBlock() throws Exception {
        User oldAdmin = user("old-admin@admin.example", "Old admin", UserRole.ADMIN, true, false);
        String stale = jwt.generateToken(oldAdmin);
        jdbc.update("update users set role = 'USER' where id = ?", oldAdmin.getId());
        mvc.perform(auth(get("/api/admin/users"), stale)).andExpect(status().isForbidden());
        jdbc.update("update users set role = 'ADMIN', blocked = true where id = ?", oldAdmin.getId());
        mvc.perform(auth(get("/api/admin/users"), stale)).andExpect(status().isUnauthorized());
    }

    @Test void listingReturnsOnlyPublicManagementFieldsWithDeterministicPagination() throws Exception {
        User first = user("first@admin.example", "First", UserRole.USER, true, false);
        User second = user("second@admin.example", "Second", UserRole.USER, true, false);
        jdbc.update("update users set created_at = ?", Timestamp.from(Instant.parse("2026-01-01T12:00:00Z")));
        String firstPage = mvc.perform(auth(get("/api/admin/users").param("size", "2"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("content", hasSize(2)))
                .andExpect(jsonPath("content[0].id").value(second.getId()))
                .andExpect(jsonPath("content[1].id").value(first.getId()))
                .andExpect(jsonPath("page").value(0)).andExpect(jsonPath("size").value(2))
                .andExpect(jsonPath("totalElements").value(3)).andExpect(jsonPath("totalPages").value(2))
                .andExpect(jsonPath("content[0].email").value(second.getEmail()))
                .andExpect(jsonPath("content[0].enabled").value(true)).andExpect(jsonPath("content[0].blocked").value(false))
                .andExpect(jsonPath("content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("content[0].tokenVersion").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(firstPage).doesNotContain("password_hash", "passwordHash", "tokenVersion", "accessToken");
        mvc.perform(auth(get("/api/admin/users").param("size", "2").param("page", "1"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("content", hasSize(1)))
                .andExpect(jsonPath("content[0].id").value(adminId));
        mvc.perform(auth(get("/api/admin/users").param("page", "50"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("content", hasSize(0)))
                .andExpect(jsonPath("totalElements").value(3));
    }

    @Test void searchIsCaseInsensitiveAndTreatsWildcardAndEscapeCharactersLiterally() throws Exception {
        user("alpha@admin.example", "ALPHA Team", UserRole.USER, true, false);
        user("literal@admin.example", "Literal %_! value", UserRole.USER, true, false);
        user("other@admin.example", "Literal ABC value", UserRole.USER, true, false);
        mvc.perform(auth(get("/api/admin/users").param("q", "  aLpHa  "), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1))
                .andExpect(jsonPath("content[0].email").value("alpha@admin.example"));
        mvc.perform(auth(get("/api/admin/users").param("q", "%_!"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1))
                .andExpect(jsonPath("content[0].displayName").value("Literal %_! value"));
        mvc.perform(auth(get("/api/admin/users").param("q", "OTHER@ADMIN.EXAMPLE"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
    }

    @Test void statusAndRoleAndQueryFiltersCombineWithoutTreatingUnconfirmedAsActive() throws Exception {
        user("active@admin.example", "Target active", UserRole.TRAINER, true, false);
        user("pending@admin.example", "Target pending", UserRole.USER, false, false);
        user("blocked@admin.example", "Target blocked", UserRole.TRAINER, true, true);
        user("blocked-pending@admin.example", "Target blocked pending", UserRole.USER, false, true);
        mvc.perform(auth(get("/api/admin/users").param("q", "Target").param("role", "TRAINER").param("status", "BLOCKED"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1))
                .andExpect(jsonPath("content[0].email").value("blocked@admin.example"));
        mvc.perform(auth(get("/api/admin/users").param("status", "PENDING"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1))
                .andExpect(jsonPath("content[0].email").value("pending@admin.example"));
        mvc.perform(auth(get("/api/admin/users").param("status", "ACTIVE"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(2));
        mvc.perform(auth(get("/api/admin/users").param("status", "BLOCKED"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "role=OWNER", "status=DISABLED", "page=bad", "page=2147483647"})
    void malformedFiltersAreRejected(String pair) throws Exception {
        String[] parameter = pair.split("=", 2);
        mvc.perform(auth(get("/api/admin/users").param(parameter[0], parameter[1]), admin))
                .andExpect(status().isBadRequest());
    }

    @Test void excessivelyLongSearchIsRejected() throws Exception {
        mvc.perform(auth(get("/api/admin/users").param("q", "a".repeat(256)), admin)).andExpect(status().isBadRequest());
    }

    @Test void roleChangesArePersistedAndInvalidateExistingSessions() throws Exception {
        User target = user("target@admin.example", "Target", UserRole.USER, true, false);
        String before = jwt.generateToken(target);
        mvc.perform(auth(patch("/api/admin/users/{id}/role", target.getId()), admin).content("{\"role\":\"TRAINER\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("role").value("TRAINER"))
                .andExpect(jsonPath("enabled").value(true)).andExpect(jsonPath("blocked").value(false));
        assertThat(users.findById(target.getId()).orElseThrow().getTokenVersion()).isEqualTo(1);
        mvc.perform(auth(get("/api/users/me"), before)).andExpect(status().isUnauthorized());
        String trainer = login(target.getEmail());
        mvc.perform(auth(get("/api/groups"), trainer)).andExpect(status().isOk());
        mvc.perform(auth(patch("/api/admin/users/{id}/role", target.getId()), admin).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("role").value("ADMIN"));
        mvc.perform(auth(get("/api/groups"), trainer)).andExpect(status().isUnauthorized());
        String promoted = login(target.getEmail());
        mvc.perform(auth(get("/api/admin/overview"), promoted)).andExpect(status().isOk());
        mvc.perform(auth(patch("/api/admin/users/{id}/role", target.getId()), admin).content("{\"role\":\"USER\"}"))
                .andExpect(status().isOk());
        mvc.perform(auth(get("/api/admin/overview"), promoted)).andExpect(status().isUnauthorized());
    }

    @Test void anAdministratorCannotChangeOwnRoleOrBlockOwnAccess() throws Exception {
        user("backup@admin.example", "Backup", UserRole.ADMIN, true, false);
        mvc.perform(auth(patch("/api/admin/users/{id}/role", adminId), admin).content("{\"role\":\"USER\"}"))
                .andExpect(status().isConflict());
        mvc.perform(auth(patch("/api/admin/users/{id}/block", adminId), admin).content("{\"blocked\":true}"))
                .andExpect(status().isConflict());
        assertThat(users.findById(adminId).orElseThrow().getTokenVersion()).isZero();
        mvc.perform(auth(get("/api/admin/overview"), admin)).andExpect(status().isOk());
    }

    @Test void repeatedNoOpChangesPreserveSessionsAndDoNotActivatePendingAccounts() throws Exception {
        User pending = user("pending@admin.example", "Pending", UserRole.USER, false, false);
        mvc.perform(auth(patch("/api/admin/users/{id}/role", pending.getId()), admin).content("{\"role\":\"USER\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("enabled").value(false));
        mvc.perform(auth(patch("/api/admin/users/{id}/block", pending.getId()), admin).content("{\"blocked\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("enabled").value(false));
        mvc.perform(auth(patch("/api/admin/users/{id}/role", adminId), admin).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk());
        mvc.perform(auth(patch("/api/admin/users/{id}/block", adminId), admin).content("{\"blocked\":false}"))
                .andExpect(status().isOk());
        assertThat(users.findById(adminId).orElseThrow().getTokenVersion()).isZero();
        assertThat(users.findById(pending.getId()).orElseThrow().getTokenVersion()).isZero();
    }

    @Test void unconfirmedAccountsCannotReceivePrivilegedRolesAndBlockedAccountsCannotBecomeAdministrators() throws Exception {
        User pending = user("pending@admin.example", "Pending", UserRole.USER, false, false);
        for (UserRole role : new UserRole[] {UserRole.TRAINER, UserRole.ADMIN}) {
            mvc.perform(auth(patch("/api/admin/users/{id}/role", pending.getId()), admin).content("{\"role\":\"" + role + "\"}"))
                    .andExpect(status().isConflict());
        }
        User blocked = user("blocked@admin.example", "Blocked", UserRole.USER, true, true);
        mvc.perform(auth(patch("/api/admin/users/{id}/role", blocked.getId()), admin).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isConflict());
        assertThat(users.findById(blocked.getId()).orElseThrow().getRole()).isEqualTo(UserRole.USER);
    }

    @Test void blockAndUnblockPreserveRoleAndPlansWhileRevokingBothEarlierSessions() throws Exception {
        User target = user("target@admin.example", "Target", UserRole.TRAINER, true, false);
        String token = jwt.generateToken(target);
        long planId = plan(token);
        mvc.perform(auth(patch("/api/admin/users/{id}/block", target.getId()), admin).content("{\"blocked\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("blocked").value(true)).andExpect(jsonPath("enabled").value(true));
        mvc.perform(auth(get("/api/plans/{id}", planId), token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody(target.getEmail())))
                .andExpect(status().isForbidden());
        mvc.perform(auth(patch("/api/admin/users/{id}/block", target.getId()), admin).content("{\"blocked\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("role").value("TRAINER"));
        mvc.perform(auth(get("/api/plans/{id}", planId), token)).andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/plans/{id}", planId), login(target.getEmail()))).andExpect(status().isOk());
        assertThat(users.findById(target.getId()).orElseThrow().getTokenVersion()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"role\":null}", "{\"role\":\"OWNER\"}"})
    void invalidRoleBodiesCannotChangeAnAccount(String body) throws Exception {
        mvc.perform(auth(patch("/api/admin/users/{id}/role", adminId), admin).content(body)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"blocked\":null}", "{\"blocked\":\"unknown\"}"})
    void invalidBlockBodiesCannotChangeAnAccount(String body) throws Exception {
        mvc.perform(auth(patch("/api/admin/users/{id}/block", adminId), admin).content(body)).andExpect(status().isBadRequest());
    }

    @Test void missingUsersReturn404AndNoAdministrativeDeleteEndpointExists() throws Exception {
        mvc.perform(auth(patch("/api/admin/users/{id}/role", Long.MAX_VALUE), admin).content("{\"role\":\"TRAINER\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(auth(patch("/api/admin/users/{id}/block", Long.MAX_VALUE), admin).content("{\"blocked\":true}"))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/admin/users/{id}", adminId), admin)).andExpect(status().isNotFound());
    }

    @Test void overviewContainsAggregateCountsAndCountsResultsAsSavedSets() throws Exception {
        User coach = user("coach@admin.example", "Coach", UserRole.TRAINER, true, false);
        user("blocked-coach@admin.example", "Blocked coach", UserRole.TRAINER, true, true);
        user("pending@admin.example", "Pending", UserRole.USER, false, false);
        String trainer = jwt.generateToken(coach);
        long planId = plan(trainer);
        String plan = mvc.perform(auth(get("/api/plans/{id}", planId), trainer)).andReturn().getResponse().getContentAsString();
        long dayId = ((Number) JsonPath.read(plan, "$.days[0].id")).longValue();
        long exerciseId = ((Number) JsonPath.read(plan, "$.days[0].exercises[0].exercise.id")).longValue();
        mvc.perform(auth(post("/api/results"), trainer).content("""
                {"time":"2026-01-01T12:00:00Z","planId":%d,"planDayId":%d,"sets":[
                {"exerciseId":%d,"setNo":1,"reps":8,"weight":20},
                {"exerciseId":%d,"setNo":2,"reps":7,"weight":20}]}
                """.formatted(planId, dayId, exerciseId, exerciseId))).andExpect(status().isCreated());
        mvc.perform(auth(post("/api/groups"), trainer).content("{\"name\":\"Group\"}")).andExpect(status().isCreated());
        mvc.perform(auth(get("/api/admin/overview"), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("users").value(4)).andExpect(jsonPath("activeUsers").value(2))
                .andExpect(jsonPath("trainers").value(2)).andExpect(jsonPath("admins").value(1))
                .andExpect(jsonPath("plans").value(1)).andExpect(jsonPath("results").value(2))
                .andExpect(jsonPath("groups").value(1)).andExpect(jsonPath("userEmails").doesNotExist());
    }

    private User user(String email, String name, UserRole role, boolean enabled, boolean blocked) {
        User user = new User();
        user.setEmail(email); user.setDisplayName(name); user.setRole(role);
        user.setPasswordHash(passwords.encode(PASSWORD));
        user.setEnabled(enabled); user.setBlocked(blocked);
        return users.saveAndFlush(user);
    }

    private long plan(String token) throws Exception {
        Long exerciseId = jdbc.queryForList("select id from exercises order by id", Long.class).getFirst();
        String created = mvc.perform(auth(post("/api/plans"), token).content("""
                {"name":"Admin test plan","days":[{"name":"Day A","exercises":[
                {"exerciseId":%d,"sets":2,"reps":8,"weight":20}]}]}
                """.formatted(exerciseId))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(created, "$.id")).longValue();
    }

    private String login(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private String loginBody(String email) { return "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"; }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token);
    }
}
