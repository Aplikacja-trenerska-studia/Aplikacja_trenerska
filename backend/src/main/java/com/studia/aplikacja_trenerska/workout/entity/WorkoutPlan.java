package com.studia.aplikacja_trenerska.workout.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.studia.aplikacja_trenerska.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "workout_plans")
public class WorkoutPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "is_shared", nullable = false)
    private boolean shared;

    @Column(name = "share_token", length = 43, unique = true)
    private String shareToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayIndex ASC")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PlanDay> days = new ArrayList<>();

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isShared() { return shared; }
    public void setShared(boolean shared) { this.shared = shared; }
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    public Instant getCreatedAt() { return createdAt; }
    public List<PlanDay> getDays() { return java.util.Collections.unmodifiableList(days); }

    public void addDay(PlanDay day) {
        if (day.getPlan() != null && day.getPlan() != this) {
            throw new IllegalArgumentException("Day already belongs to another plan");
        }
        if (!days.contains(day)) {
            days.add(day);
            day.setPlan(this);
        }
    }

    public void removeDay(PlanDay day) {
        if (days.remove(day)) day.setPlan(null);
    }
}
