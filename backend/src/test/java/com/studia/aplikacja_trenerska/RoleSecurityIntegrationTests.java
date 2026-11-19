package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studia.aplikacja_trenerska.auth.service.JwtService;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RoleSecurityIntegrationTests.AccessProbe.class)
class RoleSecurityIntegrationTests {
    private static final String EMAIL = "roles@example.com";
    private static final String PASSWORD = "RolePassword123!";
    private static final String[] TRAINER_PATHS = {
        "/api/trainer", "/api/trainer/probe", "/api/groups", "/api/groups/1/members",
        "/api/security-test/trainer"
    };

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwords;
    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("delete from verification_tokens");
        jdbc.update("delete from users");
    }

    @Test
    void registrationCannotChooseTrainerOrAdminRole() throws Exception {
        for (String role : new String[] {"TRAINER", "ADMIN"}) {
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s@example.com","password":"%s","displayName":"Athlete","role":"%s"}
                        """.formatted(role.toLowerCase(), PASSWORD, role)))
                    .andExpect(status().isCreated());
            assertThat(users.findByEmailIgnoreCase(role.toLowerCase() + "@example.com").orElseThrow().getRole())
                    .isEqualTo(UserRole.USER);
        }
    }

    @Test
    void trainerEndpointsRequireAuthentication() throws Exception {
        for (String path : TRAINER_PATHS) {
            request(path, null).andExpect(status().isUnauthorized());
            request(path, "invalid-jwt").andExpect(status().isUnauthorized());
        }
    }

    @Test
    void userCanAccessOwnProfileButCannotAccessTrainerEndpoints() throws Exception {
        String token = createToken(UserRole.USER);
        request("/api/users/me", token).andExpect(status().isOk()).andExpect(jsonPath("role").value("USER"));
        for (String path : TRAINER_PATHS) {
            request(path, token).andExpect(status().isForbidden()).andExpect(jsonPath("message").value("Brak uprawnień"));
        }
    }

    @Test
    void trainerCanAccessProfileAndTrainerEndpoints() throws Exception {
        String token = createToken(UserRole.TRAINER);
        request("/api/users/me", token).andExpect(status().isOk()).andExpect(jsonPath("role").value("TRAINER"));
        assertThat(jwtService.parseToken(token).get("role", String.class)).isEqualTo("TRAINER");
        for (String path : TRAINER_PATHS) request(path, token).andExpect(status().isOk());
    }

    @Test
    void promotionAndRevocationInvalidateExistingSessions() throws Exception {
        String userToken = createToken(UserRole.USER);
        changeRole(UserRole.TRAINER);
        request("/api/users/me", userToken).andExpect(status().isUnauthorized());
        String trainerToken = loginToken();
        request("/api/security-test/trainer", trainerToken).andExpect(status().isOk());

        changeRole(UserRole.USER);
        for (String path : TRAINER_PATHS) request(path, trainerToken).andExpect(status().isUnauthorized());
        String newUserToken = loginToken();
        request("/api/users/me", newUserToken).andExpect(status().isOk());
        request("/api/security-test/trainer", newUserToken).andExpect(status().isForbidden());
    }

    @Test
    void currentDatabaseRoleOverridesStaleTrainerClaim() throws Exception {
        String token = createToken(UserRole.TRAINER);
        // Even an out-of-band role change without session revocation cannot preserve trainer access.
        jdbc.update("update users set role = 'USER' where email = ?", EMAIL);
        for (String path : TRAINER_PATHS) request(path, token).andExpect(status().isForbidden());
        request("/api/users/me", token).andExpect(status().isOk()).andExpect(jsonPath("role").value("USER"));
    }

    @Test
    void disabledTrainerCannotUseExistingToken() throws Exception {
        String token = createToken(UserRole.TRAINER);
        jdbc.update("update users set enabled = false where email = ?", EMAIL);
        for (String path : TRAINER_PATHS) request(path, token).andExpect(status().isUnauthorized());
    }

    private String createToken(UserRole role) {
        User user = new User();
        user.setEmail(EMAIL);
        user.setDisplayName("Role test");
        user.setPasswordHash(passwords.encode(PASSWORD));
        user.setRole(role);
        return jwtService.generateToken(users.saveAndFlush(user));
    }

    private void changeRole(UserRole role) {
        jdbc.update("update users set role = ?, token_version = token_version + 1 where email = ?", role.name(), EMAIL);
    }

    private String loginToken() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var matcher = java.util.regex.Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private ResultActions request(String path, String token) throws Exception {
        var builder = get(path);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return mvc.perform(builder);
    }

    // Test-only routes exercise URL rules and method security independently.
    @RestController
    @TestComponent
    public static class AccessProbe {
        @GetMapping({"/api/trainer", "/api/trainer/probe", "/api/groups/1/members"})
        public String urlGuard() { return "ok"; }

        @GetMapping("/api/security-test/trainer")
        @PreAuthorize("hasRole('TRAINER')")
        public String methodGuard() { return "ok"; }
    }
}
