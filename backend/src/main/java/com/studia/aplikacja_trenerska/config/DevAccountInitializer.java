package com.studia.aplikacja_trenerska.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

/** Creates a local test account only when the explicitly selected dev profile is active. */
@Component
@Profile("dev")
public class DevAccountInitializer implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public DevAccountInitializer(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByEmailIgnoreCase("dev@example.test")) return;
        User user = new User();
        user.setEmail("dev@example.test");
        user.setDisplayName("Dev");
        user.setPasswordHash(passwords.encode("devdevdev"));
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        users.save(user);
    }
}
