package com.studia.aplikacja_trenerska.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.mail")
public record MailDeliveryProperties(@DefaultValue("smtp") Mode mode) {
    public MailDeliveryProperties {
        if (mode == null) {
            throw new IllegalArgumentException("app.mail.mode must be smtp or log");
        }
    }

    public enum Mode { SMTP, LOG }
}
