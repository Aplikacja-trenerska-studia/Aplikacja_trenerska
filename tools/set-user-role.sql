-- Operator-only: run with psql -X -v ON_ERROR_STOP=1 -v email=... -v role=TRAINER
-- USER revokes trainer privileges. Never expose this script through an HTTP endpoint.
\set ON_ERROR_STOP on
BEGIN;

-- Invalid roles, missing/inactive accounts or accounts outside USER/TRAINER
-- produce no row: \gset then fails and psql disconnects, rolling back.
SELECT id FROM users
WHERE lower(email) = lower(trim(:'email'))
  AND enabled = TRUE
  AND blocked = FALSE
  AND role IN ('USER', 'TRAINER')
  AND :'role' IN ('USER', 'TRAINER')
FOR UPDATE
\gset account_

UPDATE users
SET role = :'role', token_version = token_version + 1
WHERE id = :account_id AND role <> :'role';

SELECT id, email, role, token_version FROM users WHERE id = :account_id;
COMMIT;
