package com.studia.aplikacja_trenerska.workout.dto;

import java.time.Instant;
import java.util.List;

public record SharedPlanResponse(String name, String description, String ownerName, Instant createdAt, boolean own,
        List<WorkoutPlanResponse.Day> days) {
}
