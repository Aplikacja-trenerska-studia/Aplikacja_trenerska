package com.studia.aplikacja_trenerska.workout.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;
import com.studia.aplikacja_trenerska.workout.dto.WorkoutPlanRequest;
import com.studia.aplikacja_trenerska.workout.dto.WorkoutPlanResponse;
import com.studia.aplikacja_trenerska.workout.dto.WorkoutPlanSummaryResponse;
import com.studia.aplikacja_trenerska.workout.entity.Exercise;
import com.studia.aplikacja_trenerska.workout.entity.PlanDay;
import com.studia.aplikacja_trenerska.workout.entity.PlanExercise;
import com.studia.aplikacja_trenerska.workout.entity.WorkoutPlan;
import com.studia.aplikacja_trenerska.workout.mapper.WorkoutPlanMapper;
import com.studia.aplikacja_trenerska.workout.repository.ExerciseRepository;
import com.studia.aplikacja_trenerska.workout.repository.WorkoutPlanRepository;

@Service
@Transactional(readOnly = true)
public class WorkoutPlanService {
    private final WorkoutPlanRepository plans;
    private final ExerciseRepository exercises;
    private final UserRepository users;
    private final WorkoutPlanMapper mapper;

    public WorkoutPlanService(WorkoutPlanRepository plans, ExerciseRepository exercises,
            UserRepository users, WorkoutPlanMapper mapper) {
        this.plans = plans;
        this.exercises = exercises;
        this.users = users;
        this.mapper = mapper;
    }

    public List<WorkoutPlanSummaryResponse> list(String email) {
        User owner = owner(email, false);
        return plans.findAllByOwnerIdOrderByCreatedAtDescIdDesc(owner.getId()).stream().map(mapper::toSummary).toList();
    }

    public WorkoutPlanResponse get(String email, Long id) {
        return mapper.toResponse(ownedPlan(id, owner(email, false)));
    }

    @Transactional
    public WorkoutPlanResponse create(String email, WorkoutPlanRequest request) {
        User owner = owner(email, true);
        Map<Long, Exercise> catalog = resolveExercises(request);
        WorkoutPlan plan = new WorkoutPlan();
        plan.setOwner(owner);
        populate(plan, request, catalog);
        return mapper.toResponse(plans.saveAndFlush(plan));
    }

    @Transactional
    public WorkoutPlanResponse update(String email, Long id, WorkoutPlanRequest request) {
        WorkoutPlan plan = ownedPlan(id, owner(email, true));
        Map<Long, Exercise> catalog = resolveExercises(request);
        for (PlanDay day : new ArrayList<>(plan.getDays())) plan.removeDay(day);
        // Remove old positions before inserting replacements with the same unique indexes.
        plans.flush();
        populate(plan, request, catalog);
        return mapper.toResponse(plans.saveAndFlush(plan));
    }

    @Transactional
    public void delete(String email, Long id) {
        plans.delete(ownedPlan(id, owner(email, true)));
        plans.flush();
    }

    private User owner(String email, boolean lock) {
        // Match account deletion/reset locking order: user first, then its plan hierarchy.
        var result = lock ? users.findForUpdateByEmail(email) : users.findByEmailIgnoreCase(email);
        return result.filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się ponownie"));
    }

    private WorkoutPlan ownedPlan(Long id, User owner) {
        return plans.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono planu"));
    }

    private Map<Long, Exercise> resolveExercises(WorkoutPlanRequest request) {
        var ids = request.days().stream().flatMap(day -> day.exercises().stream())
                .map(entry -> entry.exerciseId()).collect(Collectors.toSet());
        var catalog = exercises.findAllById(ids).stream().collect(Collectors.toMap(Exercise::getId, Function.identity()));
        if (catalog.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan zawiera nieistniejące ćwiczenie");
        }
        return catalog;
    }

    private void populate(WorkoutPlan plan, WorkoutPlanRequest request, Map<Long, Exercise> catalog) {
        plan.setName(request.name().strip());
        plan.setDescription(request.description() == null ? null : request.description().strip());
        for (int dayIndex = 0; dayIndex < request.days().size(); dayIndex++) {
            var inputDay = request.days().get(dayIndex);
            PlanDay day = new PlanDay();
            day.setName(inputDay.name().strip());
            day.setDayIndex(dayIndex);
            plan.addDay(day);
            for (int orderIndex = 0; orderIndex < inputDay.exercises().size(); orderIndex++) {
                var input = inputDay.exercises().get(orderIndex);
                PlanExercise entry = new PlanExercise();
                entry.setExercise(catalog.get(input.exerciseId()));
                entry.setSets(input.sets());
                entry.setReps(input.reps());
                entry.setWeight(input.weight());
                entry.setOrderIndex(orderIndex);
                day.addExercise(entry);
            }
        }
    }
}
