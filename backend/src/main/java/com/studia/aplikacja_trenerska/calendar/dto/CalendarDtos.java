package com.studia.aplikacja_trenerska.calendar.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class CalendarDtos {
    private CalendarDtos() { }

    public record ScheduleRequest(
            @NotNull LocalDate date,
            @NotNull @Positive Long planId,
            @NotNull @Positive Long planDayId,
            @Size(max = 500) String note) { }

    public record ScheduledWorkoutResponse(Long id, LocalDate date, Long planId, Long planDayId, String planName,
            String dayName, String note, boolean completed, Instant completedAt) { }

    public record AgendaResponse(LocalDate date, List<ScheduledWorkoutResponse> today,
            List<ScheduledWorkoutResponse> overdue, List<ScheduledWorkoutResponse> upcoming) { }
}
