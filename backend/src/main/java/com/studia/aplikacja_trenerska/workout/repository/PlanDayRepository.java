package com.studia.aplikacja_trenerska.workout.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studia.aplikacja_trenerska.workout.entity.PlanDay;

public interface PlanDayRepository extends JpaRepository<PlanDay, Long> {
    List<PlanDay> findAllByPlanIdOrderByDayIndexAsc(Long planId);
}
