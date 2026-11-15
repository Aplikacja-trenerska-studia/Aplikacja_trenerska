package com.studia.aplikacja_trenerska.workout.dto;

import java.time.Instant;

public record WorkoutPlanSummaryResponse(Long id, String name, String description, Instant createdAt) {
}
