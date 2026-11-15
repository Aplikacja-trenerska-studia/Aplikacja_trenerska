CREATE TABLE exercises (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(160) NOT NULL,
    muscle_group    VARCHAR(32) NOT NULL,
    type            VARCHAR(16) NOT NULL,
    description     VARCHAR(2000),
    created_by      BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_exercises_name CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_exercises_muscle_group CHECK (muscle_group IN (
        'CHEST', 'BACK', 'SHOULDERS', 'BICEPS', 'TRICEPS', 'FOREARMS',
        'QUADRICEPS', 'HAMSTRINGS', 'GLUTES', 'CALVES', 'CORE', 'FULL_BODY')),
    CONSTRAINT chk_exercises_type CHECK (type IN ('STRENGTH', 'HOME'))
);

CREATE INDEX idx_exercises_filters ON exercises (muscle_group, type);
CREATE INDEX idx_exercises_created_by ON exercises (created_by);

CREATE TABLE workout_plans (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    description     VARCHAR(2000),
    is_shared       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_workout_plans_name CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE INDEX idx_workout_plans_owner ON workout_plans (owner_id, created_at, id);

CREATE TABLE plan_days (
    id              BIGSERIAL PRIMARY KEY,
    plan_id         BIGINT NOT NULL REFERENCES workout_plans (id) ON DELETE CASCADE,
    day_index       INTEGER NOT NULL,
    name            VARCHAR(120) NOT NULL,
    CONSTRAINT uq_plan_days_position UNIQUE (plan_id, day_index),
    CONSTRAINT chk_plan_days_index CHECK (day_index >= 0),
    CONSTRAINT chk_plan_days_name CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE TABLE plan_exercises (
    id              BIGSERIAL PRIMARY KEY,
    plan_day_id     BIGINT NOT NULL REFERENCES plan_days (id) ON DELETE CASCADE,
    exercise_id     BIGINT NOT NULL REFERENCES exercises (id) ON DELETE RESTRICT,
    sets            INTEGER NOT NULL,
    reps            INTEGER NOT NULL,
    weight          NUMERIC(7, 2) NOT NULL DEFAULT 0,
    order_index     INTEGER NOT NULL,
    CONSTRAINT uq_plan_exercises_position UNIQUE (plan_day_id, order_index),
    CONSTRAINT chk_plan_exercises_sets CHECK (sets > 0),
    CONSTRAINT chk_plan_exercises_reps CHECK (reps > 0),
    CONSTRAINT chk_plan_exercises_weight CHECK (weight >= 0),
    CONSTRAINT chk_plan_exercises_index CHECK (order_index >= 0)
);

CREATE INDEX idx_plan_exercises_exercise ON plan_exercises (exercise_id);
