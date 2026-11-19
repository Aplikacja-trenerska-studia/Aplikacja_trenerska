package com.studia.aplikacja_trenerska.progress.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExerciseResultResponse(UUID id, Instant time, Long exerciseId, String exerciseName,
        int setNo, int reps, BigDecimal weight, String planName, String dayName) { }
