package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "app.account.frontend-url=https://trainer-example.vercel.app",
    "app.cors.allowed-origins=https://trainer-preview.vercel.app",
    "app.cors.allowed-origin-patterns="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeploymentCorsIntegrationTests {
    private static final String FRONTEND = "https://trainer-example.vercel.app";

    @Autowired MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {FRONTEND, "https://trainer-preview.vercel.app"})
    void preflightAllowsOnlyConfiguredFrontendOriginsAndBearerHeaders(String origin) throws Exception {
        var response = mvc.perform(options("/api/plans")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andReturn().getResponse();
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .containsIgnoringCase("authorization").containsIgnoringCase("content-type");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("POST");
    }

    @Test
    void allowedFrontendCanReadHealthWithoutAnAccessToken() throws Exception {
        mvc.perform(get("/api/health").header(HttpHeaders.ORIGIN, FRONTEND))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND))
                .andExpect(jsonPath("status").value("UP"));
    }

    @Test
    void unauthorizedApiResponseRemainsReadableToTheAllowedFrontend() throws Exception {
        mvc.perform(get("/api/users/me").header(HttpHeaders.ORIGIN, FRONTEND))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND))
                .andExpect(jsonPath("message").exists());
    }

    @Test
    void allowedLoginPostReachesValidationInsteadOfBeingBlockedByCors() throws Exception {
        mvc.perform(post("/api/auth/login").header(HttpHeaders.ORIGIN, FRONTEND)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND))
                .andExpect(jsonPath("errors.email").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://another-tenant.vercel.app", "https://trainer-example.vercel.app.attacker.test",
            "http://trainer-example.vercel.app", "http://localhost:5173"})
    void preflightRejectsOtherTenantsLookalikesAndLocalOrigins(String origin) throws Exception {
        mvc.perform(options("/api/plans")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void actualRequestFromAnUnlistedTenantIsAlsoRejected() throws Exception {
        mvc.perform(get("/api/health").header(HttpHeaders.ORIGIN, "https://another-tenant.vercel.app"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
