package com.studia.aplikacja_trenerska.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionConfig {

    public ProductionConfig(Environment environment, JwtProperties jwt, AccountProperties account, CorsProperties cors,
            MailDeliveryProperties mail) {
        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> profile.equals("dev") || profile.equals("docker"))) {
            throw new IllegalArgumentException("The prod profile must not be combined with dev or docker");
        }
        if (jwt.secret() == null || jwt.secret().getBytes(StandardCharsets.UTF_8).length < 32
                || jwt.secret().startsWith("dev-") || jwt.secret().startsWith("test-")
                || jwt.secret().startsWith("REPLACE_")) {
            throw new IllegalArgumentException("Production requires a new APP_JWT_SECRET of at least 32 bytes");
        }
        if (!CorsProperties.normalizeOrigin(account.frontendUrl()).startsWith("https://")
                || cors.allowedOrigins().stream().anyMatch(origin -> !origin.startsWith("https://"))
                || !cors.allowedOriginPatterns().isEmpty()) {
            throw new IllegalArgumentException("Production requires exact HTTPS frontend/CORS origins");
        }
        for (String property : new String[] {
                "spring.datasource.url", "spring.datasource.username", "spring.datasource.password",
                "spring.flyway.url", "spring.flyway.user", "spring.flyway.password"}) {
            if (environment.getProperty(property, "").isBlank()) {
                throw new IllegalArgumentException("Missing production setting: " + property);
            }
        }
        if (mail.mode() == MailDeliveryProperties.Mode.SMTP) {
            for (String property : new String[] {"spring.mail.host", "spring.mail.username", "spring.mail.password"}) {
                if (environment.getProperty(property, "").isBlank()) {
                    throw new IllegalArgumentException("Missing production setting: " + property);
                }
            }
            if (account.mailFrom() == null || account.mailFrom().isBlank()) {
                throw new IllegalArgumentException("Missing production setting: APP_MAIL_FROM");
            }
        }
    }
}
