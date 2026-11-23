package com.studia.aplikacja_trenerska.workout.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studia.aplikacja_trenerska.workout.entity.WorkoutPlan;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
    List<WorkoutPlan> findAllByOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId);

    Optional<WorkoutPlan> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<WorkoutPlan> findByShareTokenAndSharedTrue(String shareToken);

    boolean existsByShareToken(String shareToken);
}
