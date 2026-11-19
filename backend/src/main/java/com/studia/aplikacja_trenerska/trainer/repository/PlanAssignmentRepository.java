package com.studia.aplikacja_trenerska.trainer.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.studia.aplikacja_trenerska.trainer.entity.PlanAssignment;

public interface PlanAssignmentRepository extends JpaRepository<PlanAssignment, Long> {
    List<PlanAssignment> findAllByUserIdOrderByAssignedAtDescIdDesc(Long userId);
    Optional<PlanAssignment> findByGroupIdAndUserIdAndSourcePlanId(Long groupId, Long userId, Long sourcePlanId);
}
