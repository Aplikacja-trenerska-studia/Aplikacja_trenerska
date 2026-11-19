package com.studia.aplikacja_trenerska.admin.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.admin.repository.AdminSql;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.entity.UserRole;

/** Call lock inside the transaction, before acquiring any user row locks. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class AdminAccessGuard {
    private final JdbcTemplate jdbc;
    private final AdminSql sql;

    public AdminAccessGuard(JdbcTemplate jdbc, AdminSql sql) {
        this.jdbc = jdbc;
        this.sql = sql;
    }

    public void lock() {
        jdbc.queryForObject("SELECT id FROM " + sql.table("admin_control") + " WHERE id = 1 FOR UPDATE", Integer.class);
    }

    public void ensureMayDelete(User target) {
        ensureMayDeactivate(target);
    }

    public void ensureMayDeactivate(User target) {
        if (target.getRole() != UserRole.ADMIN || !target.isEnabled() || target.isBlocked()) return;
        Long activeAdmins = jdbc.queryForObject("SELECT COUNT(*) FROM " + sql.table("users")
                + " WHERE role = 'ADMIN' AND enabled = TRUE AND blocked = FALSE", Long.class);
        if (activeAdmins == null || activeAdmins <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nie można odebrać dostępu ostatniemu aktywnemu administratorowi. Najpierw wyznacz kolejnego administratora.");
        }
    }
}
