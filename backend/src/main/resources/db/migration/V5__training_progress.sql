-- The time column is part of each primary key so both tables can become
-- TimescaleDB hypertables without changing the application contract.
CREATE TABLE exercise_results (
    id              UUID NOT NULL,
    time            TIMESTAMPTZ NOT NULL,
    user_id         BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exercise_id     BIGINT NOT NULL REFERENCES exercises (id) ON DELETE RESTRICT,
    exercise_name   VARCHAR(160) NOT NULL,
    plan_name       VARCHAR(120) NOT NULL,
    day_name        VARCHAR(120) NOT NULL,
    set_no          INTEGER NOT NULL,
    reps            INTEGER NOT NULL,
    weight          NUMERIC(7, 2) NOT NULL,
    PRIMARY KEY (id, time),
    CONSTRAINT chk_results_set_no CHECK (set_no BETWEEN 1 AND 100),
    CONSTRAINT chk_results_reps CHECK (reps BETWEEN 1 AND 1000),
    CONSTRAINT chk_results_weight CHECK (weight BETWEEN 0 AND 2000)
);

CREATE INDEX idx_results_user_time ON exercise_results (user_id, time DESC);
CREATE INDEX idx_results_user_exercise_time ON exercise_results (user_id, exercise_id, time DESC);
CREATE INDEX idx_results_exercise ON exercise_results (exercise_id);

CREATE TABLE body_metrics (
    id              UUID NOT NULL,
    time            TIMESTAMPTZ NOT NULL,
    user_id         BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    weight_kg       NUMERIC(6, 2),
    chest_cm        NUMERIC(6, 2),
    waist_cm        NUMERIC(6, 2),
    hips_cm         NUMERIC(6, 2),
    arm_cm          NUMERIC(6, 2),
    thigh_cm        NUMERIC(6, 2),
    PRIMARY KEY (id, time),
    CONSTRAINT chk_metrics_weight CHECK (weight_kg BETWEEN 10 AND 500),
    CONSTRAINT chk_metrics_chest CHECK (chest_cm BETWEEN 10 AND 300),
    CONSTRAINT chk_metrics_waist CHECK (waist_cm BETWEEN 10 AND 300),
    CONSTRAINT chk_metrics_hips CHECK (hips_cm BETWEEN 10 AND 300),
    CONSTRAINT chk_metrics_arm CHECK (arm_cm BETWEEN 5 AND 150),
    CONSTRAINT chk_metrics_thigh CHECK (thigh_cm BETWEEN 5 AND 200),
    CONSTRAINT chk_metrics_present CHECK (weight_kg IS NOT NULL OR chest_cm IS NOT NULL
        OR waist_cm IS NOT NULL OR hips_cm IS NOT NULL OR arm_cm IS NOT NULL OR thigh_cm IS NOT NULL)
);

CREATE INDEX idx_metrics_user_time ON body_metrics (user_id, time DESC);
