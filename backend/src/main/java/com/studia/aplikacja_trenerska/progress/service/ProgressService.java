package com.studia.aplikacja_trenerska.progress.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.progress.dto.BodyMetricRequest;
import com.studia.aplikacja_trenerska.progress.dto.BodyMetricResponse;
import com.studia.aplikacja_trenerska.progress.dto.ExerciseResultResponse;
import com.studia.aplikacja_trenerska.progress.dto.WorkoutResultRequest;
import com.studia.aplikacja_trenerska.progress.entity.BodyMetric;
import com.studia.aplikacja_trenerska.progress.entity.ExerciseResult;
import com.studia.aplikacja_trenerska.progress.repository.BodyMetricRepository;
import com.studia.aplikacja_trenerska.progress.repository.ExerciseResultRepository;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;
import com.studia.aplikacja_trenerska.workout.entity.Exercise;
import com.studia.aplikacja_trenerska.workout.entity.PlanExercise;
import com.studia.aplikacja_trenerska.workout.repository.WorkoutPlanRepository;

@Service
@Transactional(readOnly = true)
public class ProgressService {
    public static final int RESULT_LIMIT = 2000;
    public static final int METRIC_LIMIT = 500;
    private static final Instant FIRST_MEASUREMENT = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant MIN_FILTER = Instant.parse("0001-01-01T00:00:00Z");
    private static final Instant MAX_FILTER = Instant.parse("9999-12-31T23:59:59.999999Z");
    private final UserRepository users;
    private final WorkoutPlanRepository plans;
    private final ExerciseResultRepository results;
    private final BodyMetricRepository metrics;

    public ProgressService(UserRepository users, WorkoutPlanRepository plans,
            ExerciseResultRepository results, BodyMetricRepository metrics) {
        this.users = users;
        this.plans = plans;
        this.results = results;
        this.metrics = metrics;
    }

    @Transactional
    public List<ExerciseResultResponse> saveWorkout(String email, WorkoutResultRequest request) {
        User owner = owner(email, true);
        Instant time = measurementTime(request.time());
        var plan = plans.findByIdAndOwnerId(request.planId(), owner.getId())
                .orElseThrow(() -> missing("Nie znaleziono planu"));
        var day = plan.getDays().stream().filter(candidate -> candidate.getId().equals(request.planDayId()))
                .findFirst().orElseThrow(() -> badRequest("Dzień nie należy do wybranego planu"));
        var catalog = day.getExercises().stream().map(PlanExercise::getExercise)
                .collect(Collectors.toMap(Exercise::getId, Function.identity(), (first, repeated) -> first));
        var positions = new HashSet<String>();
        // Resolve the whole session before saving: one invalid set must not leave a partial workout.
        var entries = request.sets().stream().map(input -> {
            Exercise exercise = catalog.get(input.exerciseId());
            if (exercise == null) throw badRequest("Ćwiczenie nie należy do wybranego dnia planu");
            if (!positions.add(input.exerciseId() + ":" + input.setNo())) {
                throw badRequest("Numer serii danego ćwiczenia nie może się powtarzać w treningu");
            }
            return new ExerciseResult(time, owner.getId(), exercise.getId(), exercise.getName(),
                    plan.getName(), day.getName(), input.setNo(), input.reps(), input.weight());
        }).toList();
        return results.saveAllAndFlush(entries).stream().map(this::response).toList();
    }

    public List<ExerciseResultResponse> listResults(String email, Long exerciseId, Instant from, Instant to) {
        return listResultsForUser(owner(email, false).getId(), exerciseId, from, to);
    }

    /** The caller must authorize access to this user before using this method (e.g. accepted trainee). */
    public List<ExerciseResultResponse> listResultsForUser(Long userId, Long exerciseId, Instant from, Instant to) {
        validateRange(from, to);
        if (exerciseId != null && exerciseId <= 0) throw badRequest("Identyfikator ćwiczenia musi być dodatni");
        Specification<ExerciseResult> spec = within(userId, from, to);
        if (exerciseId != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("exerciseId"), exerciseId));
        var order = Sort.by(Sort.Order.desc("time"), Sort.Order.asc("exerciseId"),
                Sort.Order.asc("setNo"), Sort.Order.asc("id"));
        return results.findAll(spec, PageRequest.of(0, RESULT_LIMIT, order)).stream().map(this::response).toList();
    }

    @Transactional
    public void deleteResult(String email, UUID id, Instant time) {
        var owner = owner(email, true);
        var result = results.findByIdAndTimeAndUserId(id, exactTime(time), owner.getId())
                .orElseThrow(() -> missing("Nie znaleziono wyniku"));
        results.delete(result);
        results.flush();
    }

    @Transactional
    public BodyMetricResponse saveMetric(String email, BodyMetricRequest request) {
        var owner = owner(email, true);
        var metric = new BodyMetric(measurementTime(request.time()), owner.getId(), request.weightKg(),
                request.chestCm(), request.waistCm(), request.hipsCm(), request.armCm(), request.thighCm());
        return response(metrics.saveAndFlush(metric));
    }

    public List<BodyMetricResponse> listMetrics(String email, Instant from, Instant to) {
        return listMetricsForUser(owner(email, false).getId(), from, to);
    }

    /** The caller must authorize access to this user before using this method. */
    public List<BodyMetricResponse> listMetricsForUser(Long userId, Instant from, Instant to) {
        validateRange(from, to);
        var order = Sort.by(Sort.Order.desc("time"), Sort.Order.asc("id"));
        return metrics.findAll(within(userId, from, to), PageRequest.of(0, METRIC_LIMIT, order))
                .stream().map(this::response).toList();
    }

    @Transactional
    public void deleteMetric(String email, UUID id, Instant time) {
        var owner = owner(email, true);
        var metric = metrics.findByIdAndTimeAndUserId(id, exactTime(time), owner.getId())
                .orElseThrow(() -> missing("Nie znaleziono pomiaru"));
        metrics.delete(metric);
        metrics.flush();
    }

    private <T> Specification<T> within(Long userId, Instant from, Instant to) {
        return (root, query, builder) -> {
            var predicate = builder.equal(root.get("userId"), userId);
            if (from != null) predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("time"), from));
            if (to != null) predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("time"), to));
            return predicate;
        };
    }

    private User owner(String email, boolean lock) {
        // Writes follow the same user-first lock order as plan edits and account deletion.
        var lookup = lock ? users.findForUpdateByEmail(email) : users.findByEmailIgnoreCase(email);
        return lookup.filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się ponownie"));
    }

    private Instant measurementTime(Instant time) {
        if (time == null || time.isBefore(FIRST_MEASUREMENT) || time.isAfter(Instant.now().plus(5, ChronoUnit.MINUTES))) {
            throw badRequest("Data pomiaru musi być między 1.01.2000 a chwilą obecną");
        }
        // PostgreSQL persists microseconds. Return precisely the timestamp used by the composite key.
        return time.truncatedTo(ChronoUnit.MICROS);
    }

    private Instant exactTime(Instant time) {
        validateRange(time, time);
        if (time == null) throw badRequest("Podaj datę wpisu");
        return time.truncatedTo(ChronoUnit.MICROS);
    }

    private void validateRange(Instant from, Instant to) {
        for (Instant boundary : new Instant[] {from, to}) {
            if (boundary != null && (boundary.isBefore(MIN_FILTER) || boundary.isAfter(MAX_FILTER))) {
                throw badRequest("Data musi należeć do zakresu lat 1–9999");
            }
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw badRequest("Początek zakresu nie może być późniejszy od końca");
        }
    }

    private ExerciseResultResponse response(ExerciseResult result) {
        return new ExerciseResultResponse(result.getId(), result.getTime(), result.getExerciseId(),
                result.getExerciseName(), result.getSetNo(), result.getReps(), result.getWeight(),
                result.getPlanName(), result.getDayName());
    }

    private BodyMetricResponse response(BodyMetric metric) {
        return new BodyMetricResponse(metric.getId(), metric.getTime(), metric.getWeightKg(), metric.getChestCm(),
                metric.getWaistCm(), metric.getHipsCm(), metric.getArmCm(), metric.getThighCm());
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException missing(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
