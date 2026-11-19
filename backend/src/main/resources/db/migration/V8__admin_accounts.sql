ALTER TABLE users ADD COLUMN blocked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_users_role_state ON users (role, enabled, blocked);
CREATE INDEX idx_users_created ON users (created_at DESC, id DESC);

-- A single locked row serializes administrator membership changes, account
-- deletion and the optional one-time bootstrap across application instances.
CREATE TABLE admin_control (
    id INTEGER PRIMARY KEY,
    bootstrap_completed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_admin_control_singleton CHECK (id = 1)
);

INSERT INTO admin_control (id, bootstrap_completed) VALUES (1, FALSE);
