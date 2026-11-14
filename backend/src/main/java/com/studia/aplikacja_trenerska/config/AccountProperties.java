package com.studia.aplikacja_trenerska.config;

import java.time.Duration;
import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.account")
public record AccountProperties(
        String frontendUrl,
        String mailFrom,
        Duration emailConfirmationTtl,
        Duration passwordResetTtl,
        Duration accountDeletionTtl
) {
    public AccountProperties {
        URI origin = URI.create(frontendUrl);
        if (origin.getHost() == null || !("http".equals(origin.getScheme()) || "https".equals(origin.getScheme()))
                || origin.getRawQuery() != null || origin.getRawFragment() != null || origin.getUserInfo() != null) {
            throw new IllegalArgumentException("app.account.frontend-url must be an absolute HTTP(S) URL");
        }
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalArgumentException("app.account.mail-from is required");
        }
        for (Duration ttl : new Duration[] {emailConfirmationTtl, passwordResetTtl, accountDeletionTtl}) {
            if (ttl == null || ttl.isNegative() || ttl.isZero()) {
                throw new IllegalArgumentException("Account token lifetimes must be positive");
            }
        }
    }
}
