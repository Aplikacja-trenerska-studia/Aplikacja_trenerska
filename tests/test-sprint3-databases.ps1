param(
    [ValidateSet('postgres', 'timescale')]
    [string] $Database = 'postgres'
)

$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $repository 'backend'
$container = 'sprint3-test-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$image = if ($Database -eq 'timescale') { 'timescale/timescaledb:latest-pg16' } else { 'postgres:17-alpine' }

try {
    docker run --detach --rm --name $container -e POSTGRES_HOST_AUTH_METHOD=trust $image | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not start the disposable database.' }
    $ready = $false
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        docker exec $container pg_isready -h 127.0.0.1 -U postgres 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'Database startup timed out.' }

    # Use the same private schema as Supabase. No application database or volume is touched.
    docker run --rm --network "container:$container" `
        --mount "type=bind,source=$backend,target=/app" `
        --mount 'type=volume,source=aplikacja-trenerska-maven-cache,target=/root/.m2' `
        --workdir /app maven:3.9.9-eclipse-temurin-21 sh ./mvnw -q test `
        '-Dspring.datasource.url=jdbc:postgresql://localhost:5432/postgres?currentSchema=training_app' `
        '-Dspring.datasource.driver-class-name=org.postgresql.Driver' `
        '-Dspring.datasource.username=postgres' '-Dspring.datasource.password=' `
        '-Dspring.flyway.default-schema=training_app' '-Dspring.flyway.schemas=training_app' `
        '-Dspring.jpa.properties.hibernate.default_schema=training_app'
    if ($LASTEXITCODE -ne 0) { throw "Backend test suite failed on $Database." }

    if ($Database -eq 'timescale') {
        $hypertables = 'SELECT count(*) FROM timescaledb_information.hypertables WHERE hypertable_schema=''training_app'' AND hypertable_name IN (''exercise_results'',''body_metrics'');' |
            docker exec -i $container psql -X -U postgres -v ON_ERROR_STOP=1 -At
        if ($LASTEXITCODE -ne 0 -or ($hypertables -join '').Trim() -ne '2') {
            throw 'Both progress tables must be real TimescaleDB hypertables.'
        }
    }
    Write-Output "PASS: full backend suite, private schema and V4 upgrade on $Database."
}
finally {
    docker stop $container 2>&1 | Out-Null
}
