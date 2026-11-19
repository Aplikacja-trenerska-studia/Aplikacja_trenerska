package com.studia.aplikacja_trenerska.admin.dto;

import java.time.Instant;
import java.util.List;

import com.studia.aplikacja_trenerska.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public final class AdminDtos {
    private AdminDtos() { }

    public enum AccountStatus { ACTIVE, PENDING, BLOCKED }

    public record UserResponse(Long id, String email, String displayName, UserRole role,
            boolean enabled, boolean blocked, Instant createdAt) { }

    public record UserPage(List<UserResponse> content, int page, int size, long totalElements, int totalPages) { }

    public record OverviewResponse(long users, long activeUsers, long trainers, long admins,
            long plans, long results, long groups) { }

    public record RoleRequest(@NotNull UserRole role) { }
    public record BlockRequest(@NotNull Boolean blocked) { }
}
