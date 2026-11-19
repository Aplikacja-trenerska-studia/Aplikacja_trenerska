package com.studia.aplikacja_trenerska.trainer.dto;

import java.time.Instant;
import java.util.List;
import jakarta.validation.constraints.*;

public final class TrainerDtos {
    private TrainerDtos() { }
    public record GroupRequest(@NotBlank @Size(max = 120) String name) { }
    public record InviteRequest(@NotBlank @Email @Size(max = 255) String email) { }
    public record AssignRequest(@NotNull @Positive Long planId, @Positive Long userId) { }
    public record MemberResponse(Long userId, String displayName, String email, String status,
            Instant invitedAt, Instant acceptedAt) { }
    public record GroupResponse(Long id, String name, Instant createdAt, List<MemberResponse> members) { }
    public record InvitationResponse(Long groupId, String groupName, String trainerName, String status,
            Instant invitedAt, Instant acceptedAt) { }
    public record AssignmentResponse(Long id, Long planId, String planName, String trainerName,
            String groupName, Long userId, String displayName, Instant assignedAt) { }
}
