package com.studia.aplikacja_trenerska.calendar.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.calendar.dto.CalendarDtos.AgendaResponse;
import com.studia.aplikacja_trenerska.calendar.dto.CalendarDtos.ScheduleRequest;
import com.studia.aplikacja_trenerska.calendar.dto.CalendarDtos.ScheduledWorkoutResponse;
import com.studia.aplikacja_trenerska.calendar.entity.ScheduledWorkout;
import com.studia.aplikacja_trenerska.calendar.repository.ScheduledWorkoutRepository;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;
import com.studia.aplikacja_trenerska.workout.entity.PlanDay;
import com.studia.aplikacja_trenerska.workout.entity.WorkoutPlan;
import com.studia.aplikacja_trenerska.workout.repository.WorkoutPlanRepository;

@Service
@Transactional(readOnly = true)
public class ScheduleService {
    public static final int MAX_RANGE_DAYS = 92;
    public static final int MAX_PER_DAY = 10;
    public static final int OVERDUE_DAYS = 14;
    public static final int UPCOMING_DAYS = 7;
    public static final int UPCOMING_LIMIT = 10;
    private static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(2100, 12, 31);
    private final ScheduledWorkoutRepository schedule;
    private final WorkoutPlanRepository plans;
    private final UserRepository users;

    public ScheduleService(ScheduledWorkoutRepository schedule, WorkoutPlanRepository plans, UserRepository users) {
        this.schedule = schedule;
        this.plans = plans;
        this.users = users;
    }

    public List<ScheduledWorkoutResponse> list(String email, LocalDate from, LocalDate to) {
        validateDate(from);
        validateDate(to);
        if (from.isAfter(to)) throw badRequest("Początek zakresu nie może być późniejszy od końca");
        if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw badRequest("Zakres kalendarza może obejmować maksymalnie " + MAX_RANGE_DAYS + " dni");
        }
        User user = user(email, false);
        return responses(user, schedule.findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAscIdAsc(user.getId(), from, to));
    }

    public AgendaResponse agenda(String email, LocalDate date) {
        validateDate(date);
        User user = user(email, false);
        var today = schedule.findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAscIdAsc(user.getId(), date, date);
        var overdue = schedule.findAllByUserIdAndScheduledDateBetweenAndCompletedAtIsNullOrderByScheduledDateAscIdAsc(
                user.getId(), date.minusDays(OVERDUE_DAYS), date.minusDays(1));
        var upcoming = schedule.findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAscIdAsc(
                user.getId(), date.plusDays(1), date.plusDays(UPCOMING_DAYS)).stream().limit(UPCOMING_LIMIT).toList();
        return new AgendaResponse(date, responses(user, today), responses(user, overdue), responses(user, upcoming));
    }

    @Transactional
    public ScheduledWorkoutResponse create(String email, ScheduleRequest request) {
        User user = user(email, true);
        validateDate(request.date());
        WorkoutPlan plan = ownedPlan(user, request.planId());
        PlanDay day = dayOf(plan, request.planDayId());
        ensureCapacity(user, request.date());
        ScheduledWorkout entry = new ScheduledWorkout();
        entry.setUserId(user.getId());
        apply(entry, request, plan, day);
        return response(schedule.saveAndFlush(entry), plan);
    }

    @Transactional
    public ScheduledWorkoutResponse update(String email, Long id, ScheduleRequest request) {
        User user = user(email, true);
        ScheduledWorkout entry = owned(user, id);
        validateDate(request.date());
        WorkoutPlan plan = ownedPlan(user, request.planId());
        PlanDay day = dayOf(plan, request.planDayId());
        if (!request.date().equals(entry.getScheduledDate())) ensureCapacity(user, request.date());
        apply(entry, request, plan, day);
        return response(schedule.saveAndFlush(entry), plan);
    }

    @Transactional
    public ScheduledWorkoutResponse setCompleted(String email, Long id, boolean completed) {
        User user = user(email, true);
        ScheduledWorkout entry = owned(user, id);
        if (completed && entry.getCompletedAt() == null) entry.setCompletedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        if (!completed) entry.setCompletedAt(null);
        schedule.saveAndFlush(entry);
        return responses(user, List.of(entry)).getFirst();
    }

    @Transactional
    public void delete(String email, Long id) {
        User user = user(email, true);
        schedule.delete(owned(user, id));
        schedule.flush();
    }

    private void apply(ScheduledWorkout entry, ScheduleRequest request, WorkoutPlan plan, PlanDay day) {
        entry.setScheduledDate(request.date());
        entry.setPlanId(plan.getId());
        entry.setDayIndex(day.getDayIndex());
        entry.setPlanName(plan.getName());
        entry.setDayName(day.getName());
        String note = request.note() == null ? null : request.note().strip();
        entry.setNote(note == null || note.isEmpty() ? null : note);
    }

    private List<ScheduledWorkoutResponse> responses(User user, List<ScheduledWorkout> entries) {
        var ids = entries.stream().map(ScheduledWorkout::getPlanId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WorkoutPlan> owned = plans.findAllById(ids).stream()
                .filter(plan -> plan.getOwner().getId().equals(user.getId()))
                .collect(Collectors.toMap(WorkoutPlan::getId, Function.identity()));
        return entries.stream().map(entry -> response(entry, entry.getPlanId() == null ? null : owned.get(entry.getPlanId())))
                .toList();
    }

    private ScheduledWorkoutResponse response(ScheduledWorkout entry, WorkoutPlan plan) {
        PlanDay day = plan == null ? null : plan.getDays().stream()
                .filter(candidate -> candidate.getDayIndex() == entry.getDayIndex()).findFirst().orElse(null);
        return new ScheduledWorkoutResponse(entry.getId(), entry.getScheduledDate(), plan == null ? null : plan.getId(),
                day == null ? null : day.getId(), plan == null ? entry.getPlanName() : plan.getName(),
                day == null ? entry.getDayName() : day.getName(), entry.getNote(), entry.getCompletedAt() != null,
                entry.getCompletedAt());
    }

    private void ensureCapacity(User user, LocalDate date) {
        if (schedule.countByUserIdAndScheduledDate(user.getId(), date) >= MAX_PER_DAY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Na jeden dzień można zaplanować maksymalnie " + MAX_PER_DAY + " treningów");
        }
    }

    private WorkoutPlan ownedPlan(User user, Long planId) {
        return plans.findByIdAndOwnerId(planId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono planu"));
    }

    private PlanDay dayOf(WorkoutPlan plan, Long planDayId) {
        return plan.getDays().stream().filter(day -> day.getId().equals(planDayId)).findFirst()
                .orElseThrow(() -> badRequest("Dzień nie należy do wybranego planu"));
    }

    private ScheduledWorkout owned(User user, Long id) {
        return schedule.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono zaplanowanego treningu"));
    }

    private void validateDate(LocalDate date) {
        if (date == null || date.isBefore(MIN_DATE) || date.isAfter(MAX_DATE)) {
            throw badRequest("Data musi mieścić się w zakresie 1.01.2000–31.12.2100");
        }
    }

    private User user(String email, boolean lock) {
        var result = lock ? users.findForUpdateByEmail(email) : users.findByEmailIgnoreCase(email);
        return result.filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się ponownie"));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
