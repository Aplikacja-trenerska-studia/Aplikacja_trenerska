package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;
import com.studia.aplikacja_trenerska.workout.entity.Exercise;
import com.studia.aplikacja_trenerska.workout.entity.ExerciseType;
import com.studia.aplikacja_trenerska.workout.entity.MuscleGroup;
import com.studia.aplikacja_trenerska.workout.entity.PlanDay;
import com.studia.aplikacja_trenerska.workout.entity.PlanExercise;
import com.studia.aplikacja_trenerska.workout.entity.WorkoutPlan;
import com.studia.aplikacja_trenerska.workout.repository.ExerciseRepository;
import com.studia.aplikacja_trenerska.workout.repository.PlanDayRepository;
import com.studia.aplikacja_trenerska.workout.repository.PlanExerciseRepository;
import com.studia.aplikacja_trenerska.workout.repository.WorkoutPlanRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkoutModelIntegrationTests {
    @Autowired UserRepository users;
    @Autowired ExerciseRepository exercises;
    @Autowired WorkoutPlanRepository plans;
    @Autowired PlanDayRepository days;
    @Autowired PlanExerciseRepository entries;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("delete from users");
        jdbc.update("delete from exercises");
    }

    @Test
    void persistsAndReloadsOrderedPlanWithDecimalAndBodyweightExercises() {
        User owner = user("owner@example.com");
        Exercise exercise = exercise(owner);
        WorkoutPlan plan = plan(owner);
        PlanDay later = day(plan, 1);
        PlanDay first = day(plan, 0);
        entry(first, exercise, 1, new BigDecimal("42.75"));
        entry(first, exercise, 0, BigDecimal.ZERO);
        entityManager.flush();
        entityManager.clear();

        WorkoutPlan loaded = plans.findByIdAndOwnerId(plan.getId(), owner.getId()).orElseThrow();
        assertThat(loaded.getName()).isEqualTo("Strength plan");
        assertThat(loaded.getDescription()).isEqualTo("Three sessions a week");
        assertThat(loaded.isShared()).isFalse();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getOwner().getEmail()).isEqualTo("owner@example.com");
        assertThat(days.findAllByPlanIdOrderByDayIndexAsc(plan.getId()))
                .extracting(PlanDay::getId).containsExactly(first.getId(), later.getId());
        var loadedEntries = entries.findAllByPlanDayIdOrderByOrderIndexAsc(first.getId());
        assertThat(loadedEntries).extracting(PlanExercise::getOrderIndex).containsExactly(0, 1);
        assertThat(loadedEntries.getFirst().getWeight()).isEqualByComparingTo("0");
        assertThat(loadedEntries.getLast().getWeight()).isEqualByComparingTo("42.75");
        assertThat(loadedEntries.getLast().getSets()).isEqualTo(3);
        assertThat(loadedEntries.getLast().getReps()).isEqualTo(10);
        Exercise loadedExercise = loadedEntries.getFirst().getExercise();
        assertThat(loadedExercise.getType()).isEqualTo(ExerciseType.STRENGTH);
        assertThat(loadedExercise.getMuscleGroup()).isEqualTo(MuscleGroup.CHEST);
        assertThat(loadedExercise.getCreatedBy().getId()).isEqualTo(owner.getId());
    }

    @Test
    void ownerScopedQueriesExcludeOtherUsersPlans() {
        User owner = user("owner@example.com");
        User other = user("other@example.com");
        WorkoutPlan own = plan(owner);
        WorkoutPlan foreign = plan(other);
        entityManager.flush();
        entityManager.clear();

        assertThat(plans.findByIdAndOwnerId(own.getId(), owner.getId())).isPresent();
        assertThat(plans.findByIdAndOwnerId(foreign.getId(), owner.getId())).isEmpty();
        assertThat(plans.findAllByOwnerIdOrderByCreatedAtDescIdDesc(owner.getId()))
                .extracting(WorkoutPlan::getId).containsExactly(own.getId());
    }

    @Test
    void deletingPlanRemovesOnlyItsDaysAndEntriesAndKeepsCatalog() {
        User owner = user("owner@example.com");
        Exercise exercise = exercise(owner);
        WorkoutPlan removed = plan(owner);
        WorkoutPlan retained = plan(owner);
        entry(day(removed, 0), exercise, 0, BigDecimal.ZERO);
        PlanDay retainedDay = day(retained, 0);
        PlanExercise retainedEntry = entry(retainedDay, exercise, 0, BigDecimal.ZERO);
        entityManager.flush();

        plans.delete(removed);
        entityManager.flush();
        entityManager.clear();
        assertThat(plans.findAll()).extracting(WorkoutPlan::getId).containsExactly(retained.getId());
        assertThat(days.findAll()).extracting(PlanDay::getId).containsExactly(retainedDay.getId());
        assertThat(entries.findAll()).extracting(PlanExercise::getId).containsExactly(retainedEntry.getId());
        assertThat(exercises.existsById(exercise.getId())).isTrue();
        assertThat(users.existsById(owner.getId())).isTrue();
    }

    @Test
    void deletingDayRemovesItsEntriesAndKeepsSiblingDay() {
        User owner = user("owner@example.com");
        Exercise exercise = exercise(owner);
        WorkoutPlan plan = plan(owner);
        PlanDay removed = day(plan, 0);
        PlanDay retained = day(plan, 1);
        entry(removed, exercise, 0, BigDecimal.ZERO);
        PlanExercise retainedEntry = entry(retained, exercise, 0, BigDecimal.ZERO);
        plan.removeDay(removed);
        entityManager.flush();
        entityManager.clear();

        assertThat(days.findAll()).extracting(PlanDay::getId).containsExactly(retained.getId());
        assertThat(entries.findAll()).extracting(PlanExercise::getId).containsExactly(retainedEntry.getId());
        assertThat(plans.existsById(plan.getId())).isTrue();
        assertThat(exercises.existsById(exercise.getId())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aggregateSaveAndOrphanRemovalKeepCatalog(boolean removeDay) {
        User owner = user("owner@example.com");
        Exercise catalogExercise = exercise(owner);
        WorkoutPlan plan = new WorkoutPlan();
        plan.setName("Aggregate plan");
        plan.setOwner(owner);
        PlanDay day = new PlanDay();
        day.setName("First day");
        plan.addDay(day);
        PlanExercise entry = new PlanExercise();
        entry.setExercise(catalogExercise);
        entry.setSets(3);
        entry.setReps(12);
        day.addExercise(entry);
        plans.saveAndFlush(plan);
        entityManager.clear();

        WorkoutPlan loaded = plans.findById(plan.getId()).orElseThrow();
        PlanDay loadedDay = loaded.getDays().getFirst();
        assertThat(loadedDay.getExercises()).hasSize(1);
        if (removeDay) loaded.removeDay(loadedDay);
        else loadedDay.removeExercise(loadedDay.getExercises().getFirst());
        entityManager.flush();
        entityManager.clear();

        assertThat(entries.count()).isZero();
        assertThat(days.count()).isEqualTo(removeDay ? 0 : 1);
        assertThat(plans.existsById(plan.getId())).isTrue();
        assertThat(exercises.existsById(catalogExercise.getId())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "update exercises set name = '   '",
        "update exercises set muscle_group = 'UNKNOWN'",
        "update exercises set type = 'UNKNOWN'",
        "update workout_plans set name = '   '",
        "update workout_plans set owner_id = null",
        "update workout_plans set owner_id = -1",
        "update plan_days set day_index = -1",
        "update plan_days set name = '   '",
        "update plan_days set plan_id = -1",
        "update plan_exercises set sets = 0",
        "update plan_exercises set reps = 0",
        "update plan_exercises set weight = -0.01",
        "update plan_exercises set weight = 100000",
        "update plan_exercises set order_index = -1",
        "update plan_exercises set exercise_id = -1",
        "update plan_exercises set plan_day_id = -1",
        "delete from exercises"
    })
    void databaseRejectsInvalidValuesAndBrokenReferences(String sql) {
        User owner = user("owner@example.com");
        entry(day(plan(owner), 0), exercise(owner), 0, BigDecimal.ZERO);
        entityManager.flush();
        assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDuplicateDayPositionsWithinPlan() {
        WorkoutPlan plan = plan(user("owner@example.com"));
        day(plan, 0);
        assertThatThrownBy(() -> day(plan, 0)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDuplicateExercisePositionsWithinDay() {
        User owner = user("owner@example.com");
        PlanDay day = day(plan(owner), 0);
        Exercise exercise = exercise(owner);
        entry(day, exercise, 0, BigDecimal.ZERO);
        assertThatThrownBy(() -> entry(day, exercise, 0, BigDecimal.ZERO))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName("Model test");
        user.setPasswordHash("unused-in-model-tests");
        return users.saveAndFlush(user);
    }

    private Exercise exercise(User creator) {
        Exercise exercise = new Exercise();
        exercise.setName("Bench press");
        exercise.setMuscleGroup(MuscleGroup.CHEST);
        exercise.setType(ExerciseType.STRENGTH);
        exercise.setDescription("Test exercise");
        exercise.setCreatedBy(creator);
        return exercises.saveAndFlush(exercise);
    }

    private WorkoutPlan plan(User owner) {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setOwner(owner);
        plan.setName("Strength plan");
        plan.setDescription("Three sessions a week");
        return plans.saveAndFlush(plan);
    }

    private PlanDay day(WorkoutPlan plan, int position) {
        PlanDay day = new PlanDay();
        plan.addDay(day);
        day.setDayIndex(position);
        day.setName("Day " + position);
        return days.saveAndFlush(day);
    }

    private PlanExercise entry(PlanDay day, Exercise exercise, int position, BigDecimal weight) {
        PlanExercise entry = new PlanExercise();
        day.addExercise(entry);
        entry.setExercise(exercise);
        entry.setOrderIndex(position);
        entry.setSets(3);
        entry.setReps(10);
        entry.setWeight(weight);
        return entries.saveAndFlush(entry);
    }
}
