package com.studia.aplikacja_trenerska.trainer.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.persistence.*;

@Entity
@Table(name = "group_members")
@IdClass(GroupMemberId.class)
public class GroupMember {
    @Id @Column(name = "group_id") private Long groupId;
    @Id @Column(name = "user_id") private Long userId;
    @Column(nullable = false, length = 16) private String status = "PENDING";
    @Column(name = "invited_at", nullable = false) private Instant invitedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    @Column(name = "accepted_at") private Instant acceptedAt;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public Instant getInvitedAt() { return invitedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public boolean isAccepted() { return "ACCEPTED".equals(status); }
    public void accept() { this.status = "ACCEPTED"; this.acceptedAt = Instant.now().truncatedTo(ChronoUnit.MICROS); }
}
