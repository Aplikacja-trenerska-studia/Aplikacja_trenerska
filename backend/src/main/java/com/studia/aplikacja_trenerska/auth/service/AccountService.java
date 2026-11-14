package com.studia.aplikacja_trenerska.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.auth.entity.VerificationToken;
import com.studia.aplikacja_trenerska.auth.entity.VerificationTokenType;
import com.studia.aplikacja_trenerska.auth.repository.VerificationTokenRepository;
import com.studia.aplikacja_trenerska.config.AccountProperties;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

@Service
@Transactional
public class AccountService {

    private static final Duration MAIL_COOLDOWN = Duration.ofSeconds(60);
    private final SecureRandom secureRandom = new SecureRandom();
    private final VerificationTokenRepository tokens;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AccountMailService mailService;
    private final AccountProperties properties;

    public AccountService(VerificationTokenRepository tokens, UserRepository users,
            PasswordEncoder passwordEncoder, AccountMailService mailService, AccountProperties properties) {
        this.tokens = tokens;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.properties = properties;
    }

    public void sendConfirmation(User user) {
        issue(user, VerificationTokenType.EMAIL_CONFIRMATION, properties.emailConfirmationTtl());
    }

    public void resendConfirmation(String email) {
        users.findForUpdateByEmail(normalizeEmail(email)).filter(user -> !user.isEnabled())
                .ifPresent(this::sendConfirmation);
    }

    public void forgotPassword(String email) {
        users.findForUpdateByEmail(normalizeEmail(email)).filter(User::isEnabled)
                .ifPresent(user -> issue(user, VerificationTokenType.PASSWORD_RESET, properties.passwordResetTtl()));
    }

    public void confirmEmail(String rawToken) {
        User user = consume(rawToken, VerificationTokenType.EMAIL_CONFIRMATION);
        user.setEnabled(true);
        tokens.deleteByUserId(user.getId());
    }

    public void resetPassword(String rawToken, String password) {
        validatePasswordBytes(password);
        User user = consume(rawToken, VerificationTokenType.PASSWORD_RESET);
        if (!user.isEnabled()) {
            throw invalidToken();
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        user.invalidateSessions();
        tokens.deleteByUserId(user.getId());
    }

    public void requestDeletion(String email, String password) {
        User user = users.findForUpdateByEmail(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się ponownie"));
        validatePasswordBytes(password);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieprawidłowe hasło");
        }
        issue(user, VerificationTokenType.ACCOUNT_DELETION, properties.accountDeletionTtl());
    }

    public void confirmDeletion(String rawToken) {
        User user = consume(rawToken, VerificationTokenType.ACCOUNT_DELETION);
        tokens.deleteByUserId(user.getId());
        users.delete(user);
    }

    private void issue(User user, VerificationTokenType type, Duration ttl) {
        Instant now = Instant.now();
        // The caller locks the user row, serializing issuance and consumption for this account.
        boolean coolingDown = tokens.findFirstByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), type)
                .filter(token -> token.getCreatedAt().plus(MAIL_COOLDOWN).isAfter(now))
                .isPresent();
        if (coolingDown) {
            return;
        }
        tokens.deleteByUserIdAndType(user.getId(), type);
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setToken(hash(rawToken));
        token.setType(type);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(ttl));
        tokens.saveAndFlush(token);
        mailService.send(user.getEmail(), rawToken, type, ttl);
    }

    private User consume(String rawToken, VerificationTokenType type) {
        if (rawToken == null || !rawToken.matches("[A-Za-z0-9_-]{43}")) {
            throw invalidToken();
        }
        String digest = hash(rawToken);
        VerificationToken candidate = tokens.findByTokenAndType(digest, type).orElseThrow(this::invalidToken);
        User user = users.findForUpdateById(candidate.getUser().getId()).orElseThrow(this::invalidToken);
        // Re-read after obtaining the user lock: another request may have already consumed it.
        VerificationToken token = tokens.findByTokenAndType(digest, type).orElseThrow(this::invalidToken);
        if (!token.getExpiresAt().isAfter(Instant.now())) {
            throw invalidToken();
        }
        return user;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static void validatePasswordBytes(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hasło może mieć maksymalnie 72 bajty UTF-8");
        }
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Link jest nieprawidłowy, wygasł lub został już wykorzystany. Poproś o nową wiadomość.");
    }
}
