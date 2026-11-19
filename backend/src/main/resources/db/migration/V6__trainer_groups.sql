CREATE TABLE training_groups (
    id BIGSERIAL PRIMARY KEY,
    trainer_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_training_group_name CHECK (LENGTH(TRIM(name)) > 0)
);
CREATE INDEX idx_training_groups_trainer ON training_groups (trainer_id, created_at, id);

CREATE TABLE group_members (
    group_id BIGINT NOT NULL REFERENCES training_groups (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    invited_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    accepted_at TIMESTAMPTZ,
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT chk_group_member_status CHECK (status IN ('PENDING', 'ACCEPTED')),
    CONSTRAINT chk_group_member_acceptance CHECK (
        (status = 'PENDING' AND accepted_at IS NULL) OR
        (status = 'ACCEPTED' AND accepted_at IS NOT NULL))
);
CREATE INDEX idx_group_members_user ON group_members (user_id, invited_at);

CREATE TABLE plan_assignments (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    source_plan_id BIGINT REFERENCES workout_plans (id) ON DELETE SET NULL,
    copied_plan_id BIGINT REFERENCES workout_plans (id) ON DELETE SET NULL,
    assigned_by BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_name VARCHAR(120) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_assignment_membership FOREIGN KEY (group_id, user_id)
        REFERENCES group_members (group_id, user_id) ON DELETE CASCADE,
    CONSTRAINT uq_assignment_source UNIQUE (group_id, user_id, source_plan_id)
);
CREATE INDEX idx_plan_assignments_user ON plan_assignments (user_id, assigned_at, id);
