package com.studia.aplikacja_trenerska.calendar.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studia.aplikacja_trenerska.calendar.entity.ScheduledWorkout;

public interface ScheduledWorkoutRepository extends JpaRepository<ScheduledWorkout, Long> {
    List<ScheduledWorkout> findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAscIdAsc(
            Long userId, LocalDate from, LocalDate to);

    List<ScheduledWorkout> findAllByUserIdAndScheduledDateBetweenAndCompletedAtIsNullOrderByScheduledDateAscIdAsc(
            Long userId, LocalDate from, LocalDate to);

    Optional<ScheduledWorkout> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndScheduledDate(Long userId, LocalDate scheduledDate);
}
