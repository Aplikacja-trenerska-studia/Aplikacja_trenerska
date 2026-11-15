package com.studia.aplikacja_trenerska.workout.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studia.aplikacja_trenerska.workout.entity.PlanExercise;

public interface PlanExerciseRepository extends JpaRepository<PlanExercise, Long> {
    List<PlanExercise> findAllByPlanDayIdOrderByOrderIndexAsc(Long planDayId);
}
