package com.studia.aplikacja_trenerska.workout.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record WorkoutPlanResponse(Long id, String name, String description, Instant createdAt, boolean shared,
        String shareToken, List<Day> days) {
    public record Day(Long id, String name, int dayIndex, List<Entry> exercises) {
    }

    public record Entry(Long id, int orderIndex, ExerciseResponse exercise, int sets, int reps, BigDecimal weight) {
    }
}
