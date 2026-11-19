package com.studia.aplikacja_trenerska.auth.service;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.auth.entity.VerificationTokenType;
import com.studia.aplikacja_trenerska.config.AccountProperties;
import com.studia.aplikacja_trenerska.config.MailDeliveryProperties;

@Service
public class AccountMailService {

    private static final Logger log = LoggerFactory.getLogger(AccountMailService.class);

    private final JavaMailSender mailSender;
    private final AccountProperties properties;
    private final MailDeliveryProperties delivery;

    public AccountMailService(ObjectProvider<JavaMailSender> mailSenderProvider, AccountProperties properties,
            MailDeliveryProperties delivery) {
        this.properties = properties;
        this.delivery = delivery;
        if (delivery.mode() == MailDeliveryProperties.Mode.SMTP) {
            if (properties.mailFrom() == null || properties.mailFrom().isBlank()) {
                throw new IllegalArgumentException("app.account.mail-from is required in SMTP mode");
            }
            this.mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                throw new IllegalStateException("SMTP mode requires a configured JavaMailSender");
            }
        } else {
            this.mailSender = null;
            log.info("Account mail delivery configured to use the log transport");
        }
    }

    public void send(String email, String token, VerificationTokenType type, Duration ttl) {
        String path = switch (type) {
            case EMAIL_CONFIRMATION -> "/confirm-email";
            case PASSWORD_RESET -> "/reset-password";
            case ACCOUNT_DELETION -> "/confirm-deletion";
        };
        String url = properties.frontendUrl().replaceAll("/+$", "") + path + "?token=" + token;
        if (delivery.mode() == MailDeliveryProperties.Mode.LOG) {
            log.info("[MAIL LINK] {} {}", type, url);
            return;
        }
        String subject = switch (type) {
            case EMAIL_CONFIRMATION -> "Potwierdź konto w aplikacji trenerskiej";
            case PASSWORD_RESET -> "Zresetuj hasło w aplikacji trenerskiej";
            case ACCOUNT_DELETION -> "Potwierdź usunięcie konta w aplikacji trenerskiej";
        };
        String introduction = switch (type) {
            case EMAIL_CONFIRMATION -> "Witaj! Potwierdź adres e-mail, aby aktywować konto.";
            case PASSWORD_RESET -> "Otrzymaliśmy prośbę o zmianę hasła do Twojego konta.";
            case ACCOUNT_DELETION -> "Otrzymaliśmy prośbę o trwałe usunięcie Twojego konta. "
                    + "Operacja jest nieodwracalna. Konto zostanie usunięte dopiero po potwierdzeniu na stronie.";
        };
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(email);
        message.setSubject(subject);
        message.setText(introduction + "\n\n" + url + "\n\nLink jest jednorazowy i ważny przez "
                + ttl.toMinutes() + " minut.\nJeśli nie wysyłano tej prośby, zignoruj wiadomość.");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            // Do not log links or tokens. The surrounding transaction rolls back on failure.
            log.warn("Account email delivery failed; check SMTP availability and configuration");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Nie udało się wysłać wiadomości. Spróbuj ponownie później.");
        }
    }
}
