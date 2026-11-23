package com.studia.aplikacja_trenerska.stats.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.studia.aplikacja_trenerska.progress.entity.ExerciseResult;
import com.studia.aplikacja_trenerska.progress.entity.TimeSeriesId;

public interface StatsRepository extends Repository<ExerciseResult, TimeSeriesId> {
    @Query("select r.time, max(r.planName), max(r.dayName), count(distinct r.exerciseId), count(r.setNo), sum(r.reps), "
            + "sum(r.reps * r.weight) from ExerciseResult r "
            + "where r.userId = :userId and r.time >= :from and r.time < :to group by r.time order by r.time")
    List<Object[]> sessionsBetween(@Param("userId") Long userId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select r.time, max(r.planName), max(r.dayName), count(distinct r.exerciseId), count(r.setNo), sum(r.reps), "
            + "sum(r.reps * r.weight) from ExerciseResult r where r.userId = :userId group by r.time order by r.time desc")
    List<Object[]> recentSessions(@Param("userId") Long userId, Pageable page);

    @Query("select r.exerciseId, r.exerciseName, r.time, r.reps, r.weight from ExerciseResult r "
            + "where r.userId = :userId order by r.time, r.exerciseId, r.setNo")
    List<Object[]> setsForRecords(@Param("userId") Long userId);
}
