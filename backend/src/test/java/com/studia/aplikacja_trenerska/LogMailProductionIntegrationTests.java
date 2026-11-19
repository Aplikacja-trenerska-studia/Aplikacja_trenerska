package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.studia.aplikacja_trenerska.auth.service.AccountMailService;
import com.studia.aplikacja_trenerska.config.AccountProperties;
import com.studia.aplikacja_trenerska.config.MailDeliveryProperties;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@SpringBootTest(properties = {
        "APP_MAIL_MODE=log",
        "APP_JWT_SECRET=production-mail-fixture-signing-secret-at-least-32-bytes",
        "APP_FRONTEND_URL=https://trainer-app.example.test",
        "SPRING_DATASOURCE_URL=jdbc:h2:mem:mail-prod;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE SCHEMA IF NOT EXISTS training_app\\\\;CREATE DOMAIN IF NOT EXISTS training_app.TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE",
        "SPRING_DATASOURCE_USERNAME=sa", "SPRING_DATASOURCE_PASSWORD=mail-db-fixture",
        "SPRING_FLYWAY_URL=jdbc:h2:mem:mail-prod;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE SCHEMA IF NOT EXISTS training_app\\\\;CREATE DOMAIN IF NOT EXISTS training_app.TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE",
        "SPRING_FLYWAY_USER=sa", "SPRING_FLYWAY_PASSWORD=mail-db-fixture",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class LogMailProductionIntegrationTests {
    @DynamicPropertySource
    static void isolatedProductionDatabase(DynamicPropertyRegistry properties) {
        // Keep this production-configuration fixture isolated even when the wider
        // suite receives command-line PostgreSQL datasource overrides.
        String url = "jdbc:h2:mem:mail-prod;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE SCHEMA IF NOT EXISTS training_app\\;"
                + "CREATE DOMAIN IF NOT EXISTS training_app.TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE";
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "sa");
        properties.add("spring.datasource.password", () -> "mail-db-fixture");
        properties.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        properties.add("spring.flyway.url", () -> url);
        properties.add("spring.flyway.user", () -> "sa");
        properties.add("spring.flyway.password", () -> "mail-db-fixture");
    }

    private static final String EMAIL = "test-athlete@example.test";
    private static final String PASSWORD = "InitialPassword123!";
    private static final String NEW_PASSWORD = "ChangedPassword456!";
    private final Logger logger = (Logger) LoggerFactory.getLogger(AccountMailService.class);
    private ListAppender<ILoggingEvent> messages;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired Environment environment;
    @Autowired AccountProperties account;
    @Autowired MailDeliveryProperties delivery;
    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    void captureMailLinks() {
        jdbc.update("delete from training_app.verification_tokens");
        jdbc.update("delete from training_app.users");
        messages = new ListAppender<>();
        messages.start();
        logger.addAppender(messages);
    }

    @AfterEach
    void detachLogCapture() {
        logger.detachAppender(messages);
        messages.stop();
    }

    @Test
    void productionStartsWithoutSmtpAndKeepsConfirmationResetAndDeletionProtected() throws Exception {
        assertThat(delivery.mode()).isEqualTo(MailDeliveryProperties.Mode.LOG);
        assertThat(account.mailFrom()).isEmpty();
        for (String variable : new String[] {"SPRING_MAIL_HOST", "SPRING_MAIL_USERNAME", "SPRING_MAIL_PASSWORD", "APP_MAIL_FROM"}) {
            assertThat(environment.getProperty(variable)).as(variable + " is not configured").isNull();
        }
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        String registration = jsonPost("/api/auth/register", """
                {"email":"%s","password":"%s","displayName":"Test athlete"}
                """.formatted(EMAIL, PASSWORD)).andExpect(status().isCreated())
                .andExpect(jsonPath("accessToken").doesNotExist()).andReturn().getResponse().getContentAsString();
        String confirmation = loggedToken("EMAIL_CONFIRMATION", "confirm-email");
        assertPrivateResponseAndHash(registration, confirmation);
        assertThat(jdbc.queryForObject("select enabled from training_app.users where email = ?", Boolean.class, EMAIL)).isFalse();
        login(PASSWORD).andExpect(status().isForbidden());

        jsonPost("/api/auth/confirm", tokenJson(confirmation)).andExpect(status().isOk());
        jsonPost("/api/auth/confirm", tokenJson(confirmation)).andExpect(status().isBadRequest());
        String oldJwt = loginJwt(PASSWORD);
        String resetResponse = jsonPost("/api/auth/forgot-password", "{\"email\":\"" + EMAIL + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String reset = loggedToken("PASSWORD_RESET", "reset-password");
        assertPrivateResponseAndHash(resetResponse, reset);
        jsonPost("/api/auth/reset-password", "{\"token\":\"%s\",\"password\":\"%s\"}".formatted(reset, NEW_PASSWORD))
                .andExpect(status().isOk());
        jsonPost("/api/auth/reset-password", "{\"token\":\"%s\",\"password\":\"%s\"}".formatted(reset, NEW_PASSWORD))
                .andExpect(status().isBadRequest());
        login(PASSWORD).andExpect(status().isUnauthorized());
        me(oldJwt).andExpect(status().isUnauthorized());

        String jwt = loginJwt(NEW_PASSWORD);
        deletionRequest(jwt, PASSWORD).andExpect(status().isBadRequest());
        assertThat(messages.list).extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.contains("[MAIL LINK] ACCOUNT_DELETION"));
        String deletionResponse = deletionRequest(jwt, NEW_PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String deletion = loggedToken("ACCOUNT_DELETION", "confirm-deletion");
        assertPrivateResponseAndHash(deletionResponse, deletion);
        me(jwt).andExpect(status().isOk());
        jsonPost("/api/auth/confirm-deletion", tokenJson(deletion)).andExpect(status().isOk());
        jsonPost("/api/auth/confirm-deletion", tokenJson(deletion)).andExpect(status().isBadRequest());
        me(jwt).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from training_app.users", Integer.class)).isZero();
        verifyNoInteractions(mailSender);
        assertThat(messages.list).extracting(ILoggingEvent::getFormattedMessage).noneMatch(message -> message.contains(EMAIL));
    }

    private ResultActions login(String password) throws Exception {
        return jsonPost("/api/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, password));
    }

    private String loginJwt(String password) throws Exception {
        String body = login(password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var matcher = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private ResultActions me(String jwt) throws Exception {
        return mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + jwt));
    }

    private ResultActions deletionRequest(String jwt, String password) throws Exception {
        return mvc.perform(post("/api/users/me/deletion-request").header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"%s\"}".formatted(password)));
    }

    private ResultActions jsonPost(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String tokenJson(String token) {
        return "{\"token\":\"%s\"}".formatted(token);
    }

    private String loggedToken(String type, String page) {
        var links = messages.list.stream().map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.startsWith("[MAIL LINK] " + type + " ")).toList();
        assertThat(links).hasSize(1);
        var matcher = Pattern.compile("https://trainer-app\\.example\\.test/" + page + "\\?token=([A-Za-z0-9_-]{43})$")
                .matcher(links.getFirst());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private void assertPrivateResponseAndHash(String response, String token) throws Exception {
        assertThat(response).doesNotContain(token, "?token=", "[MAIL LINK]");
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        assertThat(jdbc.queryForObject("select count(*) from training_app.verification_tokens where token = ?", Integer.class, token)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from training_app.verification_tokens where token = ?", Integer.class, hash)).isEqualTo(1);
    }
}
