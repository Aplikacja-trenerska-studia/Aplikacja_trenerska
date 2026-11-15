$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $repository 'backend'
$container = 'workout-model-test-' + [guid]::NewGuid().ToString('N').Substring(0, 12)

function Invoke-UpgradeSql([string] $Sql, [string] $Database = 'workout_upgrade') {
    $output = $Sql | docker exec -i $container psql -X -U postgres -d $Database -v ON_ERROR_STOP=1 -At
    if ($LASTEXITCODE -ne 0) { throw 'Migration upgrade check failed.' }
    return ($output -join "`n").Trim()
}

try {
    # No published ports; the JDK container later shares only this temporary database's network.
    docker run --detach --rm --name $container -e POSTGRES_HOST_AUTH_METHOD=trust postgres:16-alpine | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not start disposable PostgreSQL.' }
    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        # The entrypoint's temporary init server accepts socket connections before restart.
        # TCP becomes available only on the final server used by the application.
        docker exec $container pg_isready -h 127.0.0.1 -U postgres 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'PostgreSQL did not become ready.' }

    Invoke-UpgradeSql 'CREATE DATABASE workout_upgrade;' 'postgres' | Out-Null
    foreach ($migration in @('V1__users_and_verification_tokens.sql', 'V2__account_security.sql')) {
        Invoke-UpgradeSql (Get-Content -LiteralPath (Join-Path $backend "src/main/resources/db/migration/$migration") -Raw) | Out-Null
    }
    Invoke-UpgradeSql @'
INSERT INTO users (email, password_hash, display_name, role, token_version)
VALUES ('existing@example.com', 'unused', 'Existing account', 'TRAINER', 7);
INSERT INTO verification_tokens (user_id, token, type, expires_at)
SELECT id, 'existing-token-hash', 'PASSWORD_RESET', NOW() + INTERVAL '30 minutes' FROM users;
'@ | Out-Null
    Invoke-UpgradeSql (Get-Content -LiteralPath (Join-Path $backend 'src/main/resources/db/migration/V3__exercises_and_workout_plans.sql') -Raw) | Out-Null
    $account = Invoke-UpgradeSql "SELECT email || ':' || role || ':' || token_version FROM users;"
    $tokenCount = Invoke-UpgradeSql 'SELECT count(*) FROM verification_tokens;'
    $newTableCount = Invoke-UpgradeSql "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('exercises', 'workout_plans', 'plan_days', 'plan_exercises');"
    if ($account -ne 'existing@example.com:TRAINER:7' -or $tokenCount -ne '1' -or $newTableCount -ne '4') {
        throw 'V3 did not preserve existing account/token data or create all workout tables.'
    }
    Write-Output 'PASS: V2 -> V3 upgrade preserves existing accounts, session versions and verification tokens.'

    Invoke-UpgradeSql @'
INSERT INTO exercises (name, muscle_group, type, description, created_by)
SELECT 'Existing custom exercise', 'CORE', 'HOME', 'Existing description', id FROM users;
INSERT INTO workout_plans (owner_id, name) SELECT id, 'Existing plan' FROM users;
INSERT INTO plan_days (plan_id, day_index, name) SELECT id, 0, 'Existing day' FROM workout_plans;
INSERT INTO plan_exercises (plan_day_id, exercise_id, sets, reps, weight, order_index)
SELECT d.id, e.id, 3, 10, 2.50, 0 FROM plan_days d CROSS JOIN exercises e;
'@ | Out-Null
    Invoke-UpgradeSql (Get-Content -LiteralPath (Join-Path $backend 'src/main/resources/db/migration/V4__seed_exercise_catalog.sql') -Raw) | Out-Null
    $catalogSize = Invoke-UpgradeSql 'SELECT count(*) FROM exercises WHERE created_by IS NULL;'
    $existingPlan = Invoke-UpgradeSql @'
SELECT e.name || ':' || pe.weight || ':' || p.name
FROM plan_exercises pe JOIN exercises e ON e.id = pe.exercise_id
JOIN plan_days d ON d.id = pe.plan_day_id JOIN workout_plans p ON p.id = d.plan_id;
'@
    if ($catalogSize -ne '48' -or $existingPlan -ne 'Existing custom exercise:2.50:Existing plan') {
        throw 'V4 did not add the complete catalog or preserve existing exercises and plan references.'
    }
    Write-Output 'PASS: V3 -> V4 adds 48 catalog exercises and preserves an existing custom exercise and plan.'

    # A separate, empty database exercises Flyway V1-V4 and JPA with the full backend suite.
    docker run --rm --network "container:$container" `
        --mount "type=bind,source=$backend,target=/app" `
        --mount 'type=volume,source=aplikacja-trenerska-maven-cache,target=/root/.m2' `
        --workdir /app eclipse-temurin:21-jdk sh ./mvnw -q test `
        '-Dspring.datasource.url=jdbc:postgresql://localhost:5432/postgres' `
        '-Dspring.datasource.driver-class-name=org.postgresql.Driver' `
        '-Dspring.datasource.username=postgres' '-Dspring.datasource.password='
    if ($LASTEXITCODE -ne 0) { throw 'Backend tests on PostgreSQL failed.' }
    Write-Output 'PASS: complete backend test suite on disposable PostgreSQL 16.'
}
finally {
    docker stop $container 2>&1 | Out-Null
}
