package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.studia.aplikacja_trenerska.auth.service.JwtService;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExerciseApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void requiresValidActiveSession() throws Exception {
        mvc.perform(get("/api/exercises")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/exercises").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        String token = token(UserRole.USER);
        jdbc.update("update users set enabled = false where email = 'exercise-api@example.com'");
        entityManager.clear();
        mvc.perform(get("/api/exercises").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void authenticatedRolesCanReadCatalogWithoutAccountData(UserRole role) throws Exception {
        mvc.perform(request(role)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(48)))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].muscleGroup").isString())
                .andExpect(jsonPath("$[0].type").isString())
                .andExpect(jsonPath("$[0].description").isString())
                .andExpect(jsonPath("$[0].createdBy").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({"muscleGroup,CHEST,4", "type,HOME,24", "type,STRENGTH,24"})
    void filtersByMuscleGroupOrType(String parameter, String value, int count) throws Exception {
        var result = mvc.perform(request(UserRole.USER).param(parameter, value))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(count)));
        for (int i = 0; i < count; i++) result.andExpect(jsonPath("$[" + i + "]." + parameter).value(value));
    }

    @Test
    void combinesAllFiltersAndNormalizesNameQuery() throws Exception {
        mvc.perform(request(UserRole.USER).param("muscleGroup", "CHEST").param("type", "HOME")
                .param("q", "  POMPKI KLASYCZNE  "))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Pompki klasyczne"));
    }

    @Test
    void searchesPolishNamesWithoutChangingDiacritics() throws Exception {
        mvc.perform(request(UserRole.USER).param("q", "ŚCIĄGANIE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Ściąganie drążka wyciągu górnego"));
    }

    @Test
    void noMatchesReturnsAnEmptyArray() throws Exception {
        mvc.perform(request(UserRole.USER).param("muscleGroup", "BACK").param("q", "Pompki"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void blankFiltersBehaveLikeNoFilters() throws Exception {
        mvc.perform(request(UserRole.USER).param("muscleGroup", "").param("type", "").param("q", "   "))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(48)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "\\"})
    void searchTreatsLikeMetacharactersAsLiteralText(String character) throws Exception {
        jdbc.update("insert into exercises (name, muscle_group, type) values (?, 'CORE', 'HOME')",
                "Literal " + character + " exercise");
        mvc.perform(request(UserRole.USER).param("q", character))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Literal " + character + " exercise"));
    }

    @ParameterizedTest
    @CsvSource({"muscleGroup,UNKNOWN", "type,UNKNOWN", "type,home"})
    void invalidEnumFiltersReturnStructuredBadRequest(String parameter, String value) throws Exception {
        mvc.perform(request(UserRole.USER).param(parameter, value))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").isString())
                .andExpect(jsonPath("errors." + parameter).isString());
    }

    @Test
    void rejectsOverlongSearchQuery() throws Exception {
        mvc.perform(request(UserRole.USER).param("q", "a".repeat(161)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message").isString());
    }

    @Test
    void ordersNamesIgnoringCaseAndUsesIdToBreakTies() throws Exception {
        jdbc.update("delete from users");
        jdbc.update("delete from exercises");
        for (String name : new String[] {"zeta", "Alpha", "alpha", "Beta"}) {
            jdbc.update("insert into exercises (name, muscle_group, type) values (?, 'CORE', 'HOME')", name);
        }
        var expectedIds = jdbc.queryForList("select id from exercises order by lower(name), id", Long.class);
        var result = mvc.perform(request(UserRole.USER)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));
        for (int i = 0; i < expectedIds.size(); i++) {
            result.andExpect(jsonPath("$[" + i + "].id").value(expectedIds.get(i)));
        }
        assertThat(expectedIds).hasSize(4);
    }

    private MockHttpServletRequestBuilder request(UserRole role) {
        return get("/api/exercises").header("Authorization", "Bearer " + token(role));
    }

    private String token(UserRole role) {
        User user = new User();
        user.setEmail("exercise-api@example.com");
        user.setDisplayName("Exercise API test");
        user.setPasswordHash("unused-in-api-tests");
        user.setRole(role);
        return jwtService.generateToken(users.saveAndFlush(user));
    }
}
