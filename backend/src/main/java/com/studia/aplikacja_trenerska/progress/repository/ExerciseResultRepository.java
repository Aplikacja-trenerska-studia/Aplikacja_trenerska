package com.studia.aplikacja_trenerska.progress.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.studia.aplikacja_trenerska.progress.entity.ExerciseResult;
import com.studia.aplikacja_trenerska.progress.entity.TimeSeriesId;

public interface ExerciseResultRepository extends JpaRepository<ExerciseResult, TimeSeriesId>, JpaSpecificationExecutor<ExerciseResult> {
    Optional<ExerciseResult> findByIdAndTimeAndUserId(UUID id, Instant time, Long userId);
}
