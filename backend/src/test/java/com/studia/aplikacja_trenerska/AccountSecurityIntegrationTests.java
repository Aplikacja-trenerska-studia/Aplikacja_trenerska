package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountSecurityIntegrationTests {

    private static final String EMAIL = "athlete@example.com";
    private static final String PASSWORD = "InitialPassword123!";
    private static final String NEW_PASSWORD = "NewPassword456!";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    void cleanDatabaseAndMailbox() {
        jdbc.update("delete from verification_tokens");
        jdbc.update("delete from users");
        reset(mailSender);
    }

    @Test
    void registrationRequiresExplicitSingleUseEmailConfirmation() throws Exception {
        register().andExpect(status().isCreated()).andExpect(jsonPath("accessToken").doesNotExist());
        assertThat(jdbc.queryForObject("select enabled from users where email = ?", Boolean.class, EMAIL))
                .isFalse();
        login(PASSWORD).andExpect(status().isForbidden());

        String token = mailToken("confirm-email");
        assertTokenStoredAsHash(token);
        mvc.perform(get("/api/auth/confirm").param("token", token)).andExpect(status().is4xxClientError());
        assertThat(jdbc.queryForObject("select enabled from users where email = ?", Boolean.class, EMAIL))
                .isFalse();

        confirmEmail(token).andExpect(status().isOk());
        login(PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("user.email").value(EMAIL));
        confirmEmail(token).andExpect(status().isBadRequest());
    }

    @Test
    void passwordResetHidesAccountExistenceAndRevokesExistingSessions() throws Exception {
        String jwt = activatedUserJwt();
        String knownResponse = forgot(EMAIL).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = mailToken("reset-password");
        String unknownResponse = forgot("missing@example.com").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(unknownResponse).isEqualTo(knownResponse);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        assertTokenStoredAsHash(token);

        resetPassword(token, NEW_PASSWORD).andExpect(status().isOk());
        resetPassword(token, "AnotherPassword123!").andExpect(status().isBadRequest());
        login(PASSWORD).andExpect(status().isUnauthorized());
        login(NEW_PASSWORD).andExpect(status().isOk());
        me(jwt).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from verification_tokens", Integer.class)).isZero();
    }

    @Test
    void expiredWrongPurposeAndInvalidTokensCannotResetPassword() throws Exception {
        register().andExpect(status().isCreated());
        String confirmation = mailToken("confirm-email");
        resetPassword(confirmation, NEW_PASSWORD).andExpect(status().isBadRequest());
        confirmEmail(confirmation).andExpect(status().isOk());
        reset(mailSender);

        forgot(EMAIL).andExpect(status().isOk());
        String resetToken = mailToken("reset-password");
        confirmEmail(resetToken).andExpect(status().isBadRequest());
        jdbc.update("update verification_tokens set expires_at = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(1)));
        resetPassword(resetToken, NEW_PASSWORD).andExpect(status().isBadRequest());
        resetPassword("invalid-token", NEW_PASSWORD).andExpect(status().isBadRequest());
        login(PASSWORD).andExpect(status().isOk());
    }

    @Test
    void resetRejectsWeakPasswordsWithoutConsumingToken() throws Exception {
        activatedUserJwt();
        forgot(EMAIL).andExpect(status().isOk());
        String token = mailToken("reset-password");
        resetPassword(token, "short").andExpect(status().isBadRequest());
        resetPassword(token, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void newerResetMailInvalidatesPreviousLinkAfterCooldown() throws Exception {
        activatedUserJwt();
        forgot(EMAIL).andExpect(status().isOk());
        String firstToken = mailToken("reset-password");
        jdbc.update("update verification_tokens set created_at = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(120)));
        forgot(EMAIL).andExpect(status().isOk());
        String latestToken = mailToken("reset-password");
        assertThat(latestToken).isNotEqualTo(firstToken);
        resetPassword(firstToken, NEW_PASSWORD).andExpect(status().isBadRequest());
        resetPassword(latestToken, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void concurrentRedemptionCanUseAResetTokenOnlyOnce() throws Exception {
        activatedUserJwt();
        forgot(EMAIL).andExpect(status().isOk());
        String token = mailToken("reset-password");
        var start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> attempt = () -> {
                start.await(10, TimeUnit.SECONDS);
                return resetPassword(token, NEW_PASSWORD).andReturn().getResponse().getStatus();
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            assertThat(java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 400);
        }
        login(NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void deletionRequiresAuthenticationPasswordAndMailConfirmation() throws Exception {
        String jwt = activatedUserJwt();
        deletionRequest(null, PASSWORD).andExpect(status().isUnauthorized());
        deletionRequest(jwt, "WrongPassword123!").andExpect(status().isBadRequest());
        verify(mailSender, times(0)).send(any(SimpleMailMessage.class));

        deletionRequest(jwt, PASSWORD).andExpect(status().isOk());
        String token = mailToken("confirm-deletion");
        me(jwt).andExpect(status().isOk());
        assertTokenStoredAsHash(token);
        mvc.perform(get("/api/auth/confirm-deletion").param("token", token))
                .andExpect(status().is4xxClientError());
        me(jwt).andExpect(status().isOk());

        confirmDeletion(token).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from verification_tokens", Integer.class)).isZero();
        confirmDeletion(token).andExpect(status().isBadRequest());
        me(jwt).andExpect(status().isUnauthorized());
        login(PASSWORD).andExpect(status().isUnauthorized());

        // Reusing an email address must never restore a deleted account's JWT.
        register().andExpect(status().isCreated());
        confirmEmail(mailToken("confirm-email")).andExpect(status().isOk());
        login(PASSWORD).andExpect(status().isOk());
        me(jwt).andExpect(status().isUnauthorized());
    }

    @Test
    void confirmedDeletionRemovesOwnedPlansAndPreservesOtherUsersAndSharedExercises() throws Exception {
        String jwt = activatedUserJwt();
        var initialCatalogIds = jdbc.queryForList("select id from exercises order by id", Long.class);
        jdbc.update("""
                insert into users (email, password_hash, display_name)
                values ('other@example.com', 'unused', 'Other athlete')
                """);
        jdbc.update("""
                insert into exercises (name, muscle_group, type, created_by)
                select 'Shared exercise', 'CHEST', 'HOME', id from users where email = ?
                """, EMAIL);
        Long sharedExerciseId = jdbc.queryForObject("select id from exercises where name = 'Shared exercise'", Long.class);
        jdbc.update("""
                insert into workout_plans (owner_id, name)
                select id, 'Plan for ' || email from users
                """);
        jdbc.update("insert into plan_days (plan_id, day_index, name) select id, 0, 'Day one' from workout_plans");
        jdbc.update("""
                insert into plan_exercises (plan_day_id, exercise_id, sets, reps, order_index)
                select d.id, e.id, 3, 10, 0 from plan_days d cross join exercises e where e.id = ?
                """, sharedExerciseId);

        deletionRequest(jwt, PASSWORD).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from workout_plans", Integer.class)).isEqualTo(2);
        confirmDeletion(mailToken("confirm-deletion")).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select email from users", String.class)).isEqualTo("other@example.com");
        assertThat(jdbc.queryForObject("select count(*) from verification_tokens", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from workout_plans", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from plan_days", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from plan_exercises", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from exercises", Integer.class)).isEqualTo(initialCatalogIds.size() + 1);
        assertThat(jdbc.queryForObject("select created_by from exercises where id = ?", Long.class, sharedExerciseId)).isNull();
        me(jwt).andExpect(status().isUnauthorized());
        // Remove only the test fixture; the starting catalog must survive account deletion.
        jdbc.update("delete from users");
        jdbc.update("delete from exercises where id = ?", sharedExerciseId);
        assertThat(jdbc.queryForList("select id from exercises order by id", Long.class)).isEqualTo(initialCatalogIds);
    }

    @Test
    void passwordResetAlsoInvalidatesPendingDeletion() throws Exception {
        String jwt = activatedUserJwt();
        deletionRequest(jwt, PASSWORD).andExpect(status().isOk());
        String deletionToken = mailToken("confirm-deletion");
        forgot(EMAIL).andExpect(status().isOk());
        resetPassword(mailToken("reset-password"), NEW_PASSWORD).andExpect(status().isOk());
        confirmDeletion(deletionToken).andExpect(status().isBadRequest());
        login(NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void expiredAndWrongPurposeTokensCannotDeleteAnAccount() throws Exception {
        String jwt = activatedUserJwt();
        forgot(EMAIL).andExpect(status().isOk());
        confirmDeletion(mailToken("reset-password")).andExpect(status().isBadRequest());
        deletionRequest(jwt, PASSWORD).andExpect(status().isOk());
        String deletionToken = mailToken("confirm-deletion");
        jdbc.update("update verification_tokens set expires_at = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(1)));
        confirmDeletion(deletionToken).andExpect(status().isBadRequest());
        me(jwt).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointsRejectMissingMalformedAndDisabledUserCredentials() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        me("not-a-jwt").andExpect(status().isUnauthorized());
        String jwt = activatedUserJwt();
        jdbc.update("update users set enabled = false where email = ?", EMAIL);
        me(jwt).andExpect(status().isUnauthorized());
    }

    @Test
    void smtpFailureDoesNotLeaveAnUnusableRegistration() throws Exception {
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));
        register().andExpect(status().isServiceUnavailable());
        assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from verification_tokens", Integer.class)).isZero();
    }

    @Test
    void smtpFailureOnResetDoesNotDiscloseAccountExistenceOrPersistAToken() throws Exception {
        activatedUserJwt();
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));
        String known = forgot(EMAIL).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String unknown = forgot("missing@example.com").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(known).isEqualTo(unknown);
        assertThat(jdbc.queryForObject("select count(*) from verification_tokens", Integer.class)).isZero();
    }

    @Test
    void confirmationCanBeResentAfterExpiryButCooldownPreventsRepeatedMail() throws Exception {
        register().andExpect(status().isCreated());
        String oldToken = mailToken("confirm-email");
        resendConfirmation(EMAIL).andExpect(status().isOk());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        jdbc.update("update verification_tokens set created_at = ?, expires_at = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(120)),
                java.sql.Timestamp.from(Instant.now().minusSeconds(1)));
        confirmEmail(oldToken).andExpect(status().isBadRequest());
        resendConfirmation(EMAIL).andExpect(status().isOk());
        String newToken = mailToken("confirm-email");
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        assertThat(newToken).isNotEqualTo(oldToken);
        confirmEmail(oldToken).andExpect(status().isBadRequest());
        confirmEmail(newToken).andExpect(status().isOk());
        login(PASSWORD).andExpect(status().isOk());
    }

    @Test
    void cooldownPreservesExistingResetLinkWithoutSendingRepeatedMail() throws Exception {
        activatedUserJwt();
        forgot(EMAIL).andExpect(status().isOk());
        String token = mailToken("reset-password");
        forgot(EMAIL).andExpect(status().isOk());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        resetPassword(token, NEW_PASSWORD).andExpect(status().isOk());
    }

    private ResultActions register() throws Exception {
        return jsonPost("/api/auth/register", """
                {"email":"%s","password":"%s","displayName":"Test Athlete"}
                """.formatted(EMAIL, PASSWORD));
    }

    private String activatedUserJwt() throws Exception {
        register().andExpect(status().isCreated());
        confirmEmail(mailToken("confirm-email")).andExpect(status().isOk());
        String response = login(PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        reset(mailSender);
        var matcher = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"").matcher(response);
        assertThat(matcher.find()).as("login returns accessToken").isTrue();
        return matcher.group(1);
    }

    private ResultActions login(String password) throws Exception {
        return jsonPost("/api/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, password));
    }

    private ResultActions forgot(String email) throws Exception {
        return jsonPost("/api/auth/forgot-password", "{\"email\":\"%s\"}".formatted(email));
    }

    private ResultActions resendConfirmation(String email) throws Exception {
        return jsonPost("/api/auth/resend-confirmation", "{\"email\":\"%s\"}".formatted(email));
    }

    private ResultActions confirmEmail(String token) throws Exception {
        return jsonPost("/api/auth/confirm", tokenJson(token));
    }

    private ResultActions confirmDeletion(String token) throws Exception {
        return jsonPost("/api/auth/confirm-deletion", tokenJson(token));
    }

    private ResultActions resetPassword(String token, String password) throws Exception {
        return jsonPost("/api/auth/reset-password", "{\"token\":\"%s\",\"password\":\"%s\"}".formatted(token, password));
    }

    private ResultActions deletionRequest(String jwt, String password) throws Exception {
        var request = post("/api/users/me/deletion-request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"%s\"}".formatted(password));
        if (jwt != null) request.header("Authorization", "Bearer " + jwt);
        return mvc.perform(request);
    }

    private ResultActions me(String jwt) throws Exception {
        return mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + jwt));
    }

    private ResultActions jsonPost(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String tokenJson(String token) {
        return "{\"token\":\"%s\"}".formatted(token);
    }

    private String mailToken(String page) {
        var messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.atLeastOnce()).send(messages.capture());
        var matcher = Pattern.compile("/" + page + "\\?token=([A-Za-z0-9_-]+)")
                .matcher(messages.getAllValues().stream().map(SimpleMailMessage::getText)
                        .reduce("", (all, message) -> all + "\n" + message));
        String result = null;
        while (matcher.find()) result = matcher.group(1);
        assertThat(result).as("mail contains a link to " + page).isNotBlank();
        return result;
    }

    private void assertTokenStoredAsHash(String rawToken) throws Exception {
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        assertThat(jdbc.queryForObject("select count(*) from verification_tokens where token = ?", Integer.class, rawToken))
                .isZero();
        assertThat(jdbc.queryForObject("select count(*) from verification_tokens where token = ?", Integer.class, hash))
                .isEqualTo(1);
    }
}
