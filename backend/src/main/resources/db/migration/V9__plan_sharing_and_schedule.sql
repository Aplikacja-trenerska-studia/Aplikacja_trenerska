ALTER TABLE workout_plans ADD COLUMN share_token VARCHAR(43);
ALTER TABLE workout_plans ADD CONSTRAINT uq_workout_plans_share_token UNIQUE (share_token);

CREATE TABLE scheduled_workouts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_id BIGINT REFERENCES workout_plans (id) ON DELETE SET NULL,
    day_index INTEGER NOT NULL,
    plan_name VARCHAR(120) NOT NULL,
    day_name VARCHAR(120) NOT NULL,
    scheduled_date DATE NOT NULL,
    note VARCHAR(500),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_scheduled_day_index CHECK (day_index >= 0),
    CONSTRAINT chk_scheduled_date CHECK (scheduled_date BETWEEN DATE '2000-01-01' AND DATE '2100-12-31')
);

CREATE INDEX idx_scheduled_user_date ON scheduled_workouts (user_id, scheduled_date, id);
CREATE INDEX idx_scheduled_plan ON scheduled_workouts (plan_id);
