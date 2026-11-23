package com.studia.aplikacja_trenerska.workout.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;
import com.studia.aplikacja_trenerska.workout.dto.PlanShareResponse;
import com.studia.aplikacja_trenerska.workout.dto.SharedPlanResponse;
import com.studia.aplikacja_trenerska.workout.dto.WorkoutPlanResponse;
import com.studia.aplikacja_trenerska.workout.entity.PlanDay;
import com.studia.aplikacja_trenerska.workout.entity.PlanExercise;
import com.studia.aplikacja_trenerska.workout.entity.WorkoutPlan;
import com.studia.aplikacja_trenerska.workout.mapper.WorkoutPlanMapper;
import com.studia.aplikacja_trenerska.workout.repository.WorkoutPlanRepository;

@Service
@Transactional(readOnly = true)
public class PlanSharingService {
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private final SecureRandom random = new SecureRandom();
    private final WorkoutPlanRepository plans;
    private final UserRepository users;
    private final WorkoutPlanMapper mapper;

    public PlanSharingService(WorkoutPlanRepository plans, UserRepository users, WorkoutPlanMapper mapper) {
        this.plans = plans;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional
    public PlanShareResponse share(String email, Long id) {
        WorkoutPlan plan = ownedPlan(id, user(email, true));
        if (!plan.isShared() || plan.getShareToken() == null) {
            if (plan.getShareToken() == null) plan.setShareToken(newToken());
            plan.setShared(true);
            plans.saveAndFlush(plan);
        }
        return new PlanShareResponse(true, plan.getShareToken());
    }

    @Transactional
    public void unshare(String email, Long id) {
        WorkoutPlan plan = ownedPlan(id, user(email, true));
        plan.setShared(false);
        plan.setShareToken(null);
        plans.saveAndFlush(plan);
    }

    public SharedPlanResponse view(String email, String token) {
        User viewer = user(email, false);
        WorkoutPlan plan = sharedPlan(token);
        User owner = plan.getOwner();
        return new SharedPlanResponse(plan.getName(), plan.getDescription(), owner.getDisplayName(), plan.getCreatedAt(),
                owner.getId().equals(viewer.getId()), mapper.toDays(plan));
    }

    @Transactional
    public WorkoutPlanResponse copy(String email, String token) {
        User recipient = user(email, true);
        WorkoutPlan source = sharedPlan(token);
        WorkoutPlan copy = new WorkoutPlan();
        copy.setOwner(recipient);
        copy.setName(source.getName());
        copy.setDescription(source.getDescription());
        for (PlanDay sourceDay : source.getDays()) {
            PlanDay day = new PlanDay();
            day.setName(sourceDay.getName());
            day.setDayIndex(sourceDay.getDayIndex());
            copy.addDay(day);
            for (PlanExercise sourceEntry : sourceDay.getExercises()) {
                PlanExercise entry = new PlanExercise();
                entry.setExercise(sourceEntry.getExercise());
                entry.setOrderIndex(sourceEntry.getOrderIndex());
                entry.setSets(sourceEntry.getSets());
                entry.setReps(sourceEntry.getReps());
                entry.setWeight(sourceEntry.getWeight());
                day.addExercise(entry);
            }
        }
        return mapper.toResponse(plans.saveAndFlush(copy));
    }

    private WorkoutPlan sharedPlan(String token) {
        if (token == null || !TOKEN.matcher(token).matches()) throw missing();
        return plans.findByShareTokenAndSharedTrue(token).filter(plan -> plan.getOwner().isActive())
                .orElseThrow(this::missing);
    }

    private WorkoutPlan ownedPlan(Long id, User owner) {
        return plans.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono planu"));
    }

    private User user(String email, boolean lock) {
        var result = lock ? users.findForUpdateByEmail(email) : users.findByEmailIgnoreCase(email);
        return result.filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się ponownie"));
    }

    private String newToken() {
        String token;
        do {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (plans.existsByShareToken(token));
        return token;
    }

    private ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Link do planu jest nieprawidłowy lub udostępnianie zostało wyłączone");
    }
}
