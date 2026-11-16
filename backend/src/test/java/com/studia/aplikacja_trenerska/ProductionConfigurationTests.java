package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import com.studia.aplikacja_trenerska.config.AccountProperties;
import com.studia.aplikacja_trenerska.config.CorsProperties;
import com.studia.aplikacja_trenerska.config.JwtProperties;
import com.studia.aplikacja_trenerska.config.ProductionConfig;

class ProductionConfigurationTests {
    private static final String FRONTEND = "https://trainer-example.vercel.app";
    private static final String SECRET = "production-only-fixture-secret-at-least-32-bytes";
    private static final String[] REQUIRED_SETTINGS = {
        "spring.datasource.url", "spring.datasource.username", "spring.datasource.password",
        "spring.flyway.url", "spring.flyway.user", "spring.flyway.password",
        "spring.mail.host", "spring.mail.username", "spring.mail.password"
    };

    @Test
    void acceptsAnExplicitProductionConfigurationWithExactHttpsOrigins() {
        assertThatCode(() -> new ProductionConfig(environment(), jwt(SECRET), account(FRONTEND),
                cors(List.of("https://trainer-preview.vercel.app"))))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "docker"})
    void productionCannotEnableLocalDevelopmentProfiles(String localProfile) {
        MockEnvironment environment = environment();
        environment.setActiveProfiles("prod", localProfile);
        assertThatIllegalArgumentException().isThrownBy(() -> create(environment))
                .withMessageContaining("must not be combined");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short", "dev-jwt-secret-min-32-znakow-dla-hs256-aplikacja-trenerska",
            "test-jwt-secret-min-32-znakow-dla-hs256-aplikacja",
            "REPLACE_WITH_A_NEW_RANDOM_SECRET_AT_LEAST_32_BYTES"})
    void productionRequiresANonDevelopmentSigningSecret(String secret) {
        assertThatIllegalArgumentException().isThrownBy(() -> new ProductionConfig(environment(), jwt(secret),
                account(FRONTEND), cors(List.of())))
                .withMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void productionRequiresHttpsForItsPrimaryFrontend() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ProductionConfig(environment(), jwt(SECRET),
                account("http://trainer-example.vercel.app"), cors(List.of())))
                .withMessageContaining("HTTPS");
    }

    @Test
    void productionRequiresHttpsForAdditionalAllowedOrigins() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ProductionConfig(environment(), jwt(SECRET),
                account(FRONTEND), cors(List.of("http://preview.example.test"))))
                .withMessageContaining("HTTPS");
    }

    @Test
    void productionCannotAllowOriginPatternsEvenForLocalhost() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ProductionConfig(environment(), jwt(SECRET),
                account(FRONTEND), new CorsProperties(List.of(), List.of("http://localhost:*"))))
                .withMessageContaining("HTTPS");
    }

    @Test
    void everyDatabaseMigrationAndSmtpSettingMustBePresentAndNonblank() {
        for (String property : REQUIRED_SETTINGS) {
            for (String invalidValue : new String[] {"", "  "}) {
                MockEnvironment environment = environment().withProperty(property, invalidValue);
                assertThatIllegalArgumentException().isThrownBy(() -> create(environment))
                        .withMessageContaining(property);
            }
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles("prod");
            for (String other : REQUIRED_SETTINGS) {
                if (!other.equals(property)) environment.withProperty(other, "configured-for-test");
            }
            assertThatIllegalArgumentException().isThrownBy(() -> create(environment))
                    .withMessageContaining(property);
        }
    }

    @Test
    void originsAreNormalizedWithoutBroadeningTheAllowlist() {
        CorsProperties properties = cors(List.of(" https://trainer-example.vercel.app/ ", ""));
        assertThat(properties.allowedOrigins()).containsExactly(FRONTEND);
        assertThat(properties.allowedOriginPatterns()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "https://*.vercel.app", "https://trainer-example.vercel.app/path",
            "https://trainer-example.vercel.app?redirect=elsewhere", "https://trainer-example.vercel.app#fragment",
            "https://user:password@trainer-example.vercel.app", "ftp://trainer-example.vercel.app",
            "https://trainer-example.vercel.app:0", "https://trainer-example.vercel.app:65536"})
    void invalidOriginsAreRejectedBeforeTheyCanBecomeCorsRules(String origin) {
        assertThatIllegalArgumentException().isThrownBy(() -> cors(List.of(origin)));
    }

    private ProductionConfig create(MockEnvironment environment) {
        return new ProductionConfig(environment, jwt(SECRET), account(FRONTEND), cors(List.of()));
    }

    private MockEnvironment environment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        for (String property : REQUIRED_SETTINGS) environment.withProperty(property, "configured-for-test");
        return environment;
    }

    private JwtProperties jwt(String secret) {
        return new JwtProperties(secret, 3_600_000);
    }

    private AccountProperties account(String frontend) {
        return new AccountProperties(frontend, "no-reply@example.test", Duration.ofHours(24),
                Duration.ofMinutes(30), Duration.ofMinutes(30));
    }

    private CorsProperties cors(List<String> origins) {
        return new CorsProperties(origins, List.of());
    }
}
