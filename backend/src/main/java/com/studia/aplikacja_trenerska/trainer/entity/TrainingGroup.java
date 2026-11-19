package com.studia.aplikacja_trenerska.trainer.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.persistence.*;

@Entity
@Table(name = "training_groups")
public class TrainingGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

    public Long getId() { return id; }
    public Long getTrainerId() { return trainerId; }
    public void setTrainerId(Long trainerId) { this.trainerId = trainerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getCreatedAt() { return createdAt; }
}
