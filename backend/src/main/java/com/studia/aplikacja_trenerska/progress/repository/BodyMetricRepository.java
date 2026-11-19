package com.studia.aplikacja_trenerska.progress.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.studia.aplikacja_trenerska.progress.entity.BodyMetric;
import com.studia.aplikacja_trenerska.progress.entity.TimeSeriesId;

public interface BodyMetricRepository extends JpaRepository<BodyMetric, TimeSeriesId>, JpaSpecificationExecutor<BodyMetric> {
    Optional<BodyMetric> findByIdAndTimeAndUserId(UUID id, Instant time, Long userId);
}
