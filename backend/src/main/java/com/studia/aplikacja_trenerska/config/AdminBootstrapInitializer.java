package com.studia.aplikacja_trenerska.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.studia.aplikacja_trenerska.admin.repository.AdminSql;
import com.studia.aplikacja_trenerska.admin.service.AdminAccessGuard;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

/** A one-time operator action; no public API, account creation or preset password. */
@Component
@Order(100)
public class AdminBootstrapInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapInitializer.class);
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final AdminAccessGuard guard;
    private final AdminSql sql;
    private final String email;

    public AdminBootstrapInitializer(UserRepository users, JdbcTemplate jdbc, AdminAccessGuard guard,
            AdminSql sql, @Value("${app.admin.bootstrap-email:}") String email) {
        this.users = users;
        this.jdbc = jdbc;
        this.guard = guard;
        this.sql = sql;
        this.email = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initialize();
    }

    @Transactional
    public void initialize() {
        if (email.isEmpty()) return;
        if (email.length() > 255 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_EMAIL must be a valid existing account email");
        }
        guard.lock();
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT bootstrap_completed FROM "
                + sql.table("admin_control") + " WHERE id = 1", Boolean.class))) return;
        Long existing = jdbc.queryForObject("SELECT COUNT(*) FROM " + sql.table("users")
                + " WHERE role = 'ADMIN'", Long.class);
        if (existing != null && existing > 0) {
            complete();
            return;
        }
        User account = users.findForUpdateByEmail(email).filter(User::isActive)
                .orElseThrow(() -> new IllegalStateException(
                        "Administrator bootstrap requires an existing confirmed, unblocked account; "
                        + "confirm the account or remove APP_BOOTSTRAP_ADMIN_EMAIL before restarting"));
        account.setRole(UserRole.ADMIN);
        account.invalidateSessions();
        users.saveAndFlush(account);
        complete();
        log.info("Initial administrator assigned to account id {}; remove APP_BOOTSTRAP_ADMIN_EMAIL", account.getId());
    }

    private void complete() {
        jdbc.update("UPDATE " + sql.table("admin_control") + " SET bootstrap_completed = TRUE WHERE id = 1");
    }
}
