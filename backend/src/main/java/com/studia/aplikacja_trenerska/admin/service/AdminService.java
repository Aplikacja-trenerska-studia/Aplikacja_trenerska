package com.studia.aplikacja_trenerska.admin.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.admin.dto.AdminDtos.*;
import com.studia.aplikacja_trenerska.admin.repository.AdminSql;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Service
@Transactional(readOnly = true)
public class AdminService {
    private final UserRepository users;
    private final AdminAccessGuard guard;
    private final JdbcTemplate jdbc;
    private final AdminSql sql;
    @PersistenceContext private EntityManager entityManager;

    public AdminService(UserRepository users, AdminAccessGuard guard, JdbcTemplate jdbc, AdminSql sql) {
        this.users = users;
        this.guard = guard;
        this.jdbc = jdbc;
        this.sql = sql;
    }

    public OverviewResponse overview(String email) {
        administrator(email);
        String accountTable = sql.table("users");
        return jdbc.queryForObject("SELECT "
                + "(SELECT COUNT(*) FROM " + accountTable + "), "
                + "(SELECT COUNT(*) FROM " + accountTable + " WHERE enabled = TRUE AND blocked = FALSE), "
                + "(SELECT COUNT(*) FROM " + accountTable + " WHERE role = 'TRAINER'), "
                + "(SELECT COUNT(*) FROM " + accountTable + " WHERE role = 'ADMIN'), "
                + "(SELECT COUNT(*) FROM " + sql.table("workout_plans") + "), "
                + "(SELECT COUNT(*) FROM " + sql.table("exercise_results") + "), "
                + "(SELECT COUNT(*) FROM " + sql.table("training_groups") + ")",
                (row, index) -> new OverviewResponse(row.getLong(1), row.getLong(2), row.getLong(3),
                        row.getLong(4), row.getLong(5), row.getLong(6), row.getLong(7)));
    }

    public UserPage list(String email, String q, UserRole role, AccountStatus status, int page, int size) {
        administrator(email);
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw badRequest("Nieprawidłowy numer strony lub rozmiar spoza zakresu 1–100");
        }
        if (q != null && q.length() > 255) throw badRequest("Wyszukiwana fraza może mieć maksymalnie 255 znaków");
        Specification<User> filter = (root, query, builder) -> builder.conjunction();
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.strip().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            filter = filter.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("email")), pattern, '!'),
                    builder.like(builder.lower(root.get("displayName")), pattern, '!')));
        }
        if (role != null) filter = filter.and((root, query, builder) -> builder.equal(root.get("role"), role));
        if (status != null) {
            filter = filter.and((root, query, builder) -> switch (status) {
                case ACTIVE -> builder.and(builder.isTrue(root.get("enabled")), builder.isFalse(root.get("blocked")));
                case PENDING -> builder.and(builder.isFalse(root.get("enabled")), builder.isFalse(root.get("blocked")));
                case BLOCKED -> builder.isTrue(root.get("blocked"));
            });
        }
        var result = users.findAll(filter, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new UserPage(result.stream().map(this::response).toList(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public UserResponse changeRole(String email, Long targetId, UserRole role) {
        var locked = lockAccounts(email, targetId);
        User actor = locked.actor();
        User target = locked.target();
        if (target.getRole() == role) return response(target);
        if (actor.getId().equals(target.getId())) throw conflict("Nie możesz zmienić własnej roli");
        if (!target.isEnabled()) throw conflict("Najpierw użytkownik musi potwierdzić swój adres e-mail");
        if (role == UserRole.ADMIN && target.isBlocked()) throw conflict("Przed nadaniem administratora odblokuj konto");
        if (target.getRole() == UserRole.ADMIN) guard.ensureMayDeactivate(target);
        target.setRole(role);
        target.invalidateSessions();
        return response(users.saveAndFlush(target));
    }

    @Transactional
    public UserResponse changeBlock(String email, Long targetId, boolean blocked) {
        var locked = lockAccounts(email, targetId);
        User actor = locked.actor();
        User target = locked.target();
        if (target.isBlocked() == blocked) return response(target);
        if (actor.getId().equals(target.getId())) throw conflict("Nie możesz zablokować własnego konta");
        if (blocked) guard.ensureMayDeactivate(target);
        target.setBlocked(blocked);
        target.invalidateSessions();
        return response(users.saveAndFlush(target));
    }

    private LockedAccounts lockAccounts(String email, Long targetId) {
        guard.lock();
        User originalActor = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się ponownie"));
        Map<Long, User> accounts = new HashMap<>();
        Stream.of(originalActor.getId(), targetId).distinct().sorted().forEach(id -> {
            User user = users.findForUpdateById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono użytkownika"));
            // The actor may have been loaded before waiting for its user lock.
            entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
            accounts.put(id, user);
        });
        User actor = accounts.get(originalActor.getId());
        requireAdministrator(actor);
        return new LockedAccounts(actor, accounts.get(targetId));
    }

    private User administrator(String email) {
        User actor = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się ponownie"));
        requireAdministrator(actor);
        return actor;
    }

    private void requireAdministrator(User actor) {
        if (!actor.isEnabled() || actor.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się ponownie");
        }
        if (actor.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień administratora");
        }
    }

    private UserResponse response(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(),
                user.isEnabled(), user.isBlocked(), user.getCreatedAt());
    }

    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private record LockedAccounts(User actor, User target) { }
}
