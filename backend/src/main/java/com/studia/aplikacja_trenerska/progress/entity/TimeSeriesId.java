package com.studia.aplikacja_trenerska.progress.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class TimeSeriesId implements Serializable {
    private UUID id;
    private Instant time;

    public TimeSeriesId() { }

    public TimeSeriesId(UUID id, Instant time) {
        this.id = id;
        this.time = time;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TimeSeriesId key && Objects.equals(id, key.id) && Objects.equals(time, key.time);
    }

    @Override
    public int hashCode() { return Objects.hash(id, time); }
}
