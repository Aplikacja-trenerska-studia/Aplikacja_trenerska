package com.studia.aplikacja_trenerska.stats.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class StatsDtos {
    private StatsDtos() { }

    public record StatsResponse(LocalDate from, LocalDate to, String zone, Summary summary, List<Week> weeks,
            List<ExerciseRecord> records) { }

    public record Summary(long sessions, long sets, long reps, BigDecimal volume, int activeWeeks,
            BigDecimal averageSessionsPerWeek) { }

    public record Week(LocalDate weekStart, long sessions, long sets, long reps, BigDecimal volume) { }

    public record ExerciseRecord(Long exerciseId, String exerciseName, BigDecimal maxWeight, Integer maxWeightReps,
            Instant maxWeightAt, BigDecimal estimatedOneRepMax, Instant estimatedOneRepMaxAt, int maxReps,
            Instant maxRepsAt, long totalSets, Instant lastPerformedAt) { }

    public record SessionResponse(Instant time, String planName, String dayName, long exercises, long sets, long reps,
            BigDecimal volume) { }
}
