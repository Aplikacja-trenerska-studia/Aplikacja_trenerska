package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;
import com.studia.aplikacja_trenerska.admin.repository.AdminSql;
import com.studia.aplikacja_trenerska.admin.service.AdminAccessGuard;
import com.studia.aplikacja_trenerska.auth.service.JwtService;
import com.studia.aplikacja_trenerska.config.AdminBootstrapInitializer;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

// HTTP calls commit separately. Direct SQL is limited to isolated fixture setup/cleanup.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminLifecycleIntegrationTests {
    private static final String PASSWORD = "InitialPassword123!";
    private static final String EMAIL = "lifecycle-user@example.test";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService jwt;
    @Autowired AdminAccessGuard guard;
    @Autowired AdminSql sql;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    void prepare() {
        cleanup();
        reset(mailSender);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("delete from users");
        jdbc.update("update admin_control set bootstrap_completed = false where id = 1");
    }

    @Test
    void blockingRevokesTheSessionAndPreventsLoginAndPasswordRecoveryMail() throws Exception {
        User admin = account("operator@example.test", UserRole.ADMIN, true, false);
        User user = account(EMAIL, UserRole.USER, true, false);
        String userJwt = loginJwt(EMAIL);
        block(admin, user, true).andExpect(status().isOk());

        login(EMAIL, PASSWORD).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/users/me"), userJwt)).andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/results"), userJwt)).andExpect(status().isUnauthorized());
        jsonPost("/api/auth/forgot-password", "{\"email\":\"" + EMAIL + "\"}").andExpect(status().isOk());
        verifyNoInteractions(mailSender);

        block(admin, user, false).andExpect(status().isOk());
        login(EMAIL, PASSWORD).andExpect(status().isOk());
        // Unblocking must not restore a token that was revoked by the block.
        mvc.perform(auth(get("/api/users/me"), userJwt)).andExpect(status().isUnauthorized());
    }

    @Test
    void confirmingEmailNeverRemovesAnAdministrativeBlock() throws Exception {
        User admin = account("operator@example.test", UserRole.ADMIN, true, false);
        jsonPost("/api/auth/register", """
                {"email":"%s","password":"%s","displayName":"Pending user"}
                """.formatted(EMAIL, PASSWORD)).andExpect(status().isCreated());
        String activation = mailToken(EMAIL, "confirm-email");
        User pending = users.findByEmailIgnoreCase(EMAIL).orElseThrow();
        block(admin, pending, true).andExpect(status().isOk());

        jsonPost("/api/auth/confirm", tokenJson(activation)).andExpect(status().isOk());
        User confirmed = users.findById(pending.getId()).orElseThrow();
        assertThat(confirmed.isEnabled()).isTrue();
        assertThat(confirmed.isBlocked()).isTrue();
        login(EMAIL, PASSWORD).andExpect(status().isForbidden());
    }

    @Test
    void unblockingAnUnconfirmedAccountDoesNotActivateItsEmail() throws Exception {
        User admin = account("operator@example.test", UserRole.ADMIN, true, false);
        User pending = account(EMAIL, UserRole.USER, false, true);

        block(admin, pending, false).andExpect(status().isOk());
        User reloaded = users.findById(pending.getId()).orElseThrow();
        assertThat(reloaded.isEnabled()).isFalse();
        assertThat(reloaded.isBlocked()).isFalse();
        login(EMAIL, PASSWORD).andExpect(status().isForbidden());
    }

    @Test
    void aResetLinkIssuedBeforeBlockingCannotChangeThePasswordOrRestoreAccess() throws Exception {
        User admin = account("operator@example.test", UserRole.ADMIN, true, false);
        User user = account(EMAIL, UserRole.USER, true, false);
        jsonPost("/api/auth/forgot-password", "{\"email\":\"" + EMAIL + "\"}").andExpect(status().isOk());
        String resetToken = mailToken(EMAIL, "reset-password");
        block(admin, user, true).andExpect(status().isOk());

        jsonPost("/api/auth/reset-password", """
                {"token":"%s","password":"ChangedPassword456!"}
                """.formatted(resetToken)).andExpect(status().isBadRequest());
        User reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(reloaded.isBlocked()).isTrue();
        assertThat(passwords.matches(PASSWORD, reloaded.getPasswordHash())).isTrue();
        login(EMAIL, "ChangedPassword456!").andExpect(status().isForbidden());
    }

    @Test
    void lastAdministratorCannotDeleteTheirAccountButMayDoSoAfterAnotherAdminExists() throws Exception {
        User first = account(EMAIL, UserRole.ADMIN, true, false);
        requestDeletion(first).andExpect(status().isOk());
        String deletionToken = mailToken(EMAIL, "confirm-deletion");

        jsonPost("/api/auth/confirm-deletion", tokenJson(deletionToken)).andExpect(status().isConflict());
        assertThat(users.existsById(first.getId())).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from verification_tokens", Integer.class)).isEqualTo(1);

        User second = account("second-admin@example.test", UserRole.ADMIN, true, false);
        jsonPost("/api/auth/confirm-deletion", tokenJson(deletionToken)).andExpect(status().isOk());
        assertThat(users.existsById(first.getId())).isFalse();
        assertThat(users.existsById(second.getId())).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isEqualTo(1);
    }

    @Test
    void blockedOrUnconfirmedAdministratorsDoNotAllowDeletingTheLastActiveAdmin() throws Exception {
        User onlyActive = account(EMAIL, UserRole.ADMIN, true, false);
        account("blocked-admin@example.test", UserRole.ADMIN, true, true);
        account("pending-admin@example.test", UserRole.ADMIN, false, false);
        requestDeletion(onlyActive).andExpect(status().isOk());

        jsonPost("/api/auth/confirm-deletion", tokenJson(mailToken(EMAIL, "confirm-deletion")))
                .andExpect(status().isConflict());
        assertThat(users.existsById(onlyActive.getId())).isTrue();
    }

    @Test
    @Timeout(30)
    void twoConcurrentAdminDeletionsLeaveOneActiveAdministrator() throws Exception {
        User first = account(EMAIL, UserRole.ADMIN, true, false);
        User second = account("second-admin@example.test", UserRole.ADMIN, true, false);
        requestDeletion(first).andExpect(status().isOk());
        requestDeletion(second).andExpect(status().isOk());
        String firstToken = mailToken(first.getEmail(), "confirm-deletion");
        String secondToken = mailToken(second.getEmail(), "confirm-deletion");
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return jsonPost("/api/auth/confirm-deletion", tokenJson(firstToken))
                        .andReturn().getResponse().getStatus();
            });
            var two = executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return jsonPost("/api/auth/confirm-deletion", tokenJson(secondToken))
                        .andReturn().getResponse().getStatus();
            });
            assertThat(List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(jdbc.queryForObject("select count(*) from users where role='ADMIN' and enabled and not blocked",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void ordinaryUserCanStillDeleteTheirAccountWithoutAnyAdministrator() throws Exception {
        User user = account(EMAIL, UserRole.USER, true, false);
        requestDeletion(user).andExpect(status().isOk());
        jsonPost("/api/auth/confirm-deletion", tokenJson(mailToken(EMAIL, "confirm-deletion")))
                .andExpect(status().isOk());
        assertThat(users.existsById(user.getId())).isFalse();
    }

    @Test
    void bootstrapPromotesOnlyTheConfiguredExistingAccountAndRevokesItsOldSession() throws Exception {
        User target = account(EMAIL, UserRole.USER, true, false);
        account("unrelated@example.test", UserRole.USER, true, false);
        String oldJwt = jwt.generateToken(target);
        String originalHash = target.getPasswordHash();

        bootstrap("  LIFECYCLE-USER@EXAMPLE.TEST ");

        User initialized = users.findById(target.getId()).orElseThrow();
        assertThat(initialized.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(initialized.getTokenVersion()).isEqualTo(target.getTokenVersion() + 1);
        assertThat(initialized.getPasswordHash()).isEqualTo(originalHash);
        assertThat(initialized.isEnabled()).isTrue();
        assertThat(initialized.isBlocked()).isFalse();
        assertThat(users.findByEmailIgnoreCase("unrelated@example.test").orElseThrow().getRole()).isEqualTo(UserRole.USER);
        assertThat(completed()).isTrue();
        mvc.perform(auth(get("/api/users/me"), oldJwt)).andExpect(status().isUnauthorized());
        login(EMAIL, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void consumedBootstrapDoesNotGrantRolesAgainOrOverwriteLaterAdministrativeChanges() {
        User target = account(EMAIL, UserRole.USER, true, false);
        User another = account("another@example.test", UserRole.USER, true, false);
        bootstrap(EMAIL);
        long version = users.findById(target.getId()).orElseThrow().getTokenVersion();
        bootstrap(EMAIL);
        assertThat(users.findById(target.getId()).orElseThrow().getTokenVersion()).isEqualTo(version);

        // Simulate a later operator recovery; the stale deployment setting is not a standing role grant.
        jdbc.update("update users set role='USER' where id=?", target.getId());
        bootstrap(another.getEmail());
        assertThat(users.findById(target.getId()).orElseThrow().getRole()).isEqualTo(UserRole.USER);
        assertThat(users.findById(another.getId()).orElseThrow().getRole()).isEqualTo(UserRole.USER);
        assertThat(completed()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"active", "blocked", "pending"})
    void existingAdministratorConsumesBootstrapWithoutPromotingAnotherAccount(String state) {
        account("existing-admin@example.test", UserRole.ADMIN, !state.equals("pending"), state.equals("blocked"));
        User target = account(EMAIL, UserRole.USER, true, false);
        bootstrap(EMAIL);
        assertThat(users.findById(target.getId()).orElseThrow().getRole()).isEqualTo(UserRole.USER);
        assertThat(completed()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "pending", "blocked"})
    void invalidBootstrapTargetFailsWithoutCreatingOrActivatingAnAccount(String state) {
        if (!state.equals("missing")) account(EMAIL, UserRole.USER, !state.equals("pending"), state.equals("blocked"));
        long initialUsers = users.count();

        assertThatThrownBy(() -> bootstrap(EMAIL)).isInstanceOf(IllegalStateException.class);

        assertThat(users.count()).isEqualTo(initialUsers);
        assertThat(jdbc.queryForObject("select count(*) from users where role='ADMIN'", Integer.class)).isZero();
        assertThat(completed()).isFalse();
        if (!state.equals("missing")) {
            User unchanged = users.findByEmailIgnoreCase(EMAIL).orElseThrow();
            assertThat(unchanged.isEnabled()).isEqualTo(!state.equals("pending"));
            assertThat(unchanged.isBlocked()).isEqualTo(state.equals("blocked"));
        }
    }

    @Test
    void absentBootstrapConfigurationDoesNotCreateAnAdminOrConsumeInitialization() {
        account(EMAIL, UserRole.USER, true, false);
        bootstrap(" ");
        assertThat(users.findByEmailIgnoreCase(EMAIL).orElseThrow().getRole()).isEqualTo(UserRole.USER);
        assertThat(completed()).isFalse();
    }

    private User account(String email, UserRole role, boolean enabled, boolean blocked) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName("Lifecycle account");
        user.setPasswordHash(passwords.encode(PASSWORD));
        user.setRole(role);
        user.setEnabled(enabled);
        user.setBlocked(blocked);
        return users.saveAndFlush(user);
    }

    private ResultActions block(User admin, User target, boolean blocked) throws Exception {
        return mvc.perform(auth(patch("/api/admin/users/{id}/block", target.getId()), jwt.generateToken(admin))
                .content("{\"blocked\":" + blocked + "}"));
    }

    private ResultActions requestDeletion(User user) throws Exception {
        return mvc.perform(auth(post("/api/users/me/deletion-request"), jwt.generateToken(user))
                .content("{\"password\":\"" + PASSWORD + "\"}"));
    }

    private String loginJwt(String email) throws Exception {
        return JsonPath.read(login(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(), "$.accessToken");
    }

    private ResultActions login(String email, String password) throws Exception {
        return jsonPost("/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private ResultActions jsonPost(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token);
    }

    private String tokenJson(String token) { return "{\"token\":\"" + token + "\"}"; }

    private String mailToken(String email, String page) {
        var captured = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeastOnce()).send(captured.capture());
        String text = captured.getAllValues().stream().filter(mail -> List.of(mail.getTo()).contains(email))
                .map(SimpleMailMessage::getText).filter(body -> body.contains("/" + page + "?token="))
                .reduce((previous, latest) -> latest).orElseThrow();
        var matcher = Pattern.compile("/" + page + "\\?token=([A-Za-z0-9_-]{43})").matcher(text);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private void bootstrap(String email) {
        var initializer = new AdminBootstrapInitializer(users, jdbc, guard, sql, email);
        new TransactionTemplate(transactions).executeWithoutResult(transaction -> initializer.initialize());
    }

    private boolean completed() {
        return Boolean.TRUE.equals(jdbc.queryForObject("select bootstrap_completed from admin_control where id=1", Boolean.class));
    }
}
