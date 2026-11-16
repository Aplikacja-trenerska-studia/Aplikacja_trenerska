package com.studia.aplikacja_trenerska.config;

import java.net.URI;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins, List<String> allowedOriginPatterns) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
                .map(String::trim).filter(value -> !value.isEmpty()).map(CorsProperties::normalizeOrigin).toList();
        allowedOriginPatterns = allowedOriginPatterns == null ? List.of() : List.copyOf(allowedOriginPatterns);
    }

    public static String normalizeOrigin(String value) {
        URI origin = URI.create(value);
        if (!("https".equals(origin.getScheme()) || "http".equals(origin.getScheme()))
                || origin.getHost() == null || origin.getUserInfo() != null || origin.getRawQuery() != null
                || origin.getRawFragment() != null || origin.getPort() == 0 || origin.getPort() > 65535
                || (origin.getRawPath() != null && !origin.getRawPath().isEmpty() && !origin.getRawPath().equals("/"))) {
            throw new IllegalArgumentException("CORS origins must be exact HTTP(S) origins without paths or wildcards");
        }
        return value.replaceAll("/+$", "");
    }
}
