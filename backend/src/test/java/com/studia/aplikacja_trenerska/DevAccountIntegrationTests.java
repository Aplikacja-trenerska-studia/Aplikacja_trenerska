package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.studia.aplikacja_trenerska.config.DevAccountInitializer;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
class DevAccountIntegrationTests {
    @Autowired DevAccountInitializer initializer;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        cleanup();
        initializer.run(null);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("delete from users");
    }

    @Test
    void devAccountIsActiveHasHashedPasswordAndCanLogIn() throws Exception {
        var user = users.findByEmailIgnoreCase("dev@example.test").orElseThrow();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getPasswordHash()).isNotEqualTo("devdevdev");
        assertThat(passwords.matches("devdevdev", user.getPasswordHash())).isTrue();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"dev@example.test\",\"password\":\"devdevdev\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("accessToken").isString())
                .andExpect(jsonPath("user.displayName").value("Dev"));
    }

    @Test
    void repeatedStartupDoesNotDuplicateOrResetTheAccount() {
        var before = users.findByEmailIgnoreCase("dev@example.test").orElseThrow();
        initializer.run(null);
        initializer.run(null);
        assertThat(users.count()).isEqualTo(1);
        var after = users.findByEmailIgnoreCase("dev@example.test").orElseThrow();
        assertThat(after.getId()).isEqualTo(before.getId());
        assertThat(after.getPasswordHash()).isEqualTo(before.getPasswordHash());
    }

    @Test
    void existingAccountIsNeverReactivatedOrOverwritten() {
        var user = users.findByEmailIgnoreCase("dev@example.test").orElseThrow();
        user.setEmail("DEV@example.test");
        user.setPasswordHash(passwords.encode("ChangedPassword123!"));
        user.setDisplayName("Changed");
        user.setRole(UserRole.TRAINER);
        user.setEnabled(false);
        user.invalidateSessions();
        users.saveAndFlush(user);
        initializer.run(null);
        var after = users.findByEmailIgnoreCase("dev@example.test").orElseThrow();
        assertThat(after.getPasswordHash()).isEqualTo(user.getPasswordHash());
        assertThat(after.getDisplayName()).isEqualTo("Changed");
        assertThat(after.getRole()).isEqualTo(UserRole.TRAINER);
        assertThat(after.isEnabled()).isFalse();
        assertThat(after.getTokenVersion()).isEqualTo(user.getTokenVersion());
    }
}
