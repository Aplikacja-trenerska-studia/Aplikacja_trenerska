package com.studia.aplikacja_trenerska.trainer.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.persistence.*;

@Entity
@Table(name = "plan_assignments")
public class PlanAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "group_id", nullable = false) private Long groupId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "source_plan_id") private Long sourcePlanId;
    @Column(name = "copied_plan_id") private Long copiedPlanId;
    @Column(name = "assigned_by", nullable = false) private Long assignedBy;
    @Column(name = "plan_name", nullable = false, length = 120) private String planName;
    @Column(name = "assigned_at", nullable = false) private Instant assignedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSourcePlanId() { return sourcePlanId; }
    public void setSourcePlanId(Long sourcePlanId) { this.sourcePlanId = sourcePlanId; }
    public Long getCopiedPlanId() { return copiedPlanId; }
    public void setCopiedPlanId(Long copiedPlanId) { this.copiedPlanId = copiedPlanId; }
    public Long getAssignedBy() { return assignedBy; }
    public void setAssignedBy(Long assignedBy) { this.assignedBy = assignedBy; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public Instant getAssignedAt() { return assignedAt; }
    public void refreshAssignedAt() { this.assignedAt = Instant.now().truncatedTo(ChronoUnit.MICROS); }
}
