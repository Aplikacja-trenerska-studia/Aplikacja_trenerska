package com.studia.aplikacja_trenerska.stats.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.stats.dto.StatsDtos.ExerciseRecord;
import com.studia.aplikacja_trenerska.stats.dto.StatsDtos.SessionResponse;
import com.studia.aplikacja_trenerska.stats.dto.StatsDtos.StatsResponse;
import com.studia.aplikacja_trenerska.stats.dto.StatsDtos.Summary;
import com.studia.aplikacja_trenerska.stats.dto.StatsDtos.Week;
import com.studia.aplikacja_trenerska.stats.repository.StatsRepository;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class StatsService {
    public static final int MAX_WEEKS = 52;
    public static final int MAX_SESSIONS = 20;
    private static final BigDecimal THIRTY = BigDecimal.valueOf(30);
    private final StatsRepository stats;
    private final UserRepository users;

    public StatsService(StatsRepository stats, UserRepository users) {
        this.stats = stats;
        this.users = users;
    }

    public StatsResponse summary(String email, int weeks, String zoneName) {
        if (weeks < 1 || weeks > MAX_WEEKS) throw badRequest("Liczba tygodni musi mieścić się w zakresie 1–" + MAX_WEEKS);
        ZoneId zone = zone(zoneName);
        User user = user(email);
        LocalDate currentWeek = LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate firstWeek = currentWeek.minusWeeks(weeks - 1L);
        Instant from = firstWeek.atStartOfDay(zone).toInstant();
        Instant to = currentWeek.plusWeeks(1).atStartOfDay(zone).toInstant();
        Map<LocalDate, long[]> counters = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> volumes = new LinkedHashMap<>();
        for (int index = 0; index < weeks; index++) {
            counters.put(firstWeek.plusWeeks(index), new long[3]);
            volumes.put(firstWeek.plusWeeks(index), BigDecimal.ZERO);
        }
        for (SessionResponse session : sessions(stats.sessionsBetween(user.getId(), from, to))) {
            LocalDate week = LocalDate.ofInstant(session.time(), zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            long[] counter = counters.get(week);
            if (counter == null) continue;
            counter[0]++;
            counter[1] += session.sets();
            counter[2] += session.reps();
            volumes.merge(week, session.volume(), BigDecimal::add);
        }
        List<Week> weekly = new ArrayList<>();
        long sessionCount = 0, setCount = 0, repCount = 0;
        BigDecimal volume = BigDecimal.ZERO;
        int activeWeeks = 0;
        for (var entry : counters.entrySet()) {
            long[] counter = entry.getValue();
            BigDecimal weekVolume = scale(volumes.get(entry.getKey()));
            weekly.add(new Week(entry.getKey(), counter[0], counter[1], counter[2], weekVolume));
            sessionCount += counter[0];
            setCount += counter[1];
            repCount += counter[2];
            volume = volume.add(weekVolume);
            if (counter[0] > 0) activeWeeks++;
        }
        BigDecimal average = BigDecimal.valueOf(sessionCount).divide(BigDecimal.valueOf(weeks), 1, RoundingMode.HALF_UP);
        Summary summary = new Summary(sessionCount, setCount, repCount, scale(volume), activeWeeks, average);
        return new StatsResponse(firstWeek, currentWeek.plusDays(6), zone.getId(), summary, weekly, records(user.getId()));
    }

    public List<SessionResponse> recentSessions(String email, int limit) {
        if (limit < 1 || limit > MAX_SESSIONS) throw badRequest("Limit musi mieścić się w zakresie 1–" + MAX_SESSIONS);
        return sessions(stats.recentSessions(user(email).getId(), PageRequest.of(0, limit)));
    }

    private List<SessionResponse> sessions(List<Object[]> rows) {
        return rows.stream().map(row -> new SessionResponse((Instant) row[0], (String) row[1], (String) row[2],
                number(row[3]), number(row[4]), number(row[5]), scale(decimal(row[6])))).toList();
    }

    private List<ExerciseRecord> records(Long userId) {
        Map<Long, RecordAccumulator> byExercise = new LinkedHashMap<>();
        for (Object[] row : stats.setsForRecords(userId)) {
            Long exerciseId = ((Number) row[0]).longValue();
            byExercise.computeIfAbsent(exerciseId, RecordAccumulator::new)
                    .add((String) row[1], (Instant) row[2], ((Number) row[3]).intValue(), decimal(row[4]));
        }
        Collator collator = Collator.getInstance(Locale.forLanguageTag("pl"));
        return byExercise.values().stream().map(RecordAccumulator::toRecord)
                .sorted(Comparator.comparing(ExerciseRecord::exerciseName, collator)).toList();
    }

    private ZoneId zone(String zoneName) {
        try {
            return ZoneId.of(zoneName);
        } catch (DateTimeException ex) {
            throw badRequest("Nieprawidłowa strefa czasowa");
        }
    }

    private User user(String email) {
        return users.findByEmailIgnoreCase(email).filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się ponownie"));
    }

    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal number) return number;
        return new BigDecimal(value.toString());
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static final class RecordAccumulator {
        private final Long exerciseId;
        private String name;
        private BigDecimal maxWeight;
        private Integer maxWeightReps;
        private Instant maxWeightAt;
        private BigDecimal oneRepMax;
        private Instant oneRepMaxAt;
        private int maxReps;
        private Instant maxRepsAt;
        private long totalSets;
        private Instant lastAt;

        RecordAccumulator(Long exerciseId) {
            this.exerciseId = exerciseId;
        }

        void add(String exerciseName, Instant time, int reps, BigDecimal weight) {
            name = exerciseName;
            lastAt = time;
            totalSets++;
            if (reps > maxReps) {
                maxReps = reps;
                maxRepsAt = time;
            }
            if (weight.signum() <= 0) return;
            int weightOrder = maxWeight == null ? 1 : weight.compareTo(maxWeight);
            if (weightOrder > 0 || (weightOrder == 0 && reps > maxWeightReps)) {
                maxWeight = weight;
                maxWeightReps = reps;
                maxWeightAt = time;
            }
            BigDecimal estimate = reps == 1 ? weight
                    : weight.multiply(THIRTY.add(BigDecimal.valueOf(reps))).divide(THIRTY, 2, RoundingMode.HALF_UP);
            if (oneRepMax == null || estimate.compareTo(oneRepMax) > 0) {
                oneRepMax = estimate;
                oneRepMaxAt = time;
            }
        }

        ExerciseRecord toRecord() {
            return new ExerciseRecord(exerciseId, name, maxWeight == null ? null : scale(maxWeight), maxWeightReps,
                    maxWeightAt, oneRepMax == null ? null : scale(oneRepMax), oneRepMaxAt, maxReps, maxRepsAt,
                    totalSets, lastAt);
        }
    }
}
