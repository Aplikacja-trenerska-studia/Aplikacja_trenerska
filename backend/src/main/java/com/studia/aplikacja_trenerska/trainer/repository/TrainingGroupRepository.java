package com.studia.aplikacja_trenerska.trainer.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.studia.aplikacja_trenerska.trainer.entity.TrainingGroup;

public interface TrainingGroupRepository extends JpaRepository<TrainingGroup, Long> {
    List<TrainingGroup> findAllByTrainerIdOrderByCreatedAtDescIdDesc(Long trainerId);
    long countByTrainerId(Long trainerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from TrainingGroup g where g.id = :id and g.trainerId = :trainerId")
    Optional<TrainingGroup> findOwnedForUpdate(@Param("id") Long id, @Param("trainerId") Long trainerId);
    Optional<TrainingGroup> findByIdAndTrainerId(Long id, Long trainerId);
}
