#requires -Version 7.0
$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot
$suffix = [guid]::NewGuid().ToString('N').Substring(0, 12)
$prefix = 'deployment-smoke-' + $suffix
$network = $prefix + '-network'
$database = $prefix + '-db'
$mailpit = $prefix + '-mail'
$backend = $prefix + '-api'
$image = 'aplikacja-trenerska-deployment-smoke:' + $suffix
$databasePassword = [guid]::NewGuid().ToString('N')
$jwtSecret = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$frontend = 'https://training-app.example.test'
$preview = 'https://training-preview.example.test'
$email = 'deployment-' + $suffix + '@example.test'
$password = 'Deployment-' + [guid]::NewGuid().ToString('N') + '!'
$createdContainers = [System.Collections.Generic.List[string]]::new()
$networkCreated = $false
$imageCreated = $false
$stage = 'Docker readiness'

function Assert-That([bool] $Condition, [string] $Message) {
    if (-not $Condition) { throw $Message }
}

function Invoke-Sql([string] $Sql) {
    $result = $Sql | docker exec -i $database psql -X -U deployment -d deployment -v ON_ERROR_STOP=1 -At
    if ($LASTEXITCODE -ne 0) { throw 'Disposable database query failed.' }
    return ($result -join "`n").Trim()
}

function Get-PublishedPort([string] $Container, [string] $ContainerPort) {
    $inspection = docker inspect $Container | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect a disposable container.' }
    return $inspection[0].NetworkSettings.Ports.$ContainerPort[0].HostPort
}

function Invoke-Api([string] $Path, [string] $Method = 'GET', $Body = $null,
        [string] $Jwt = '', [string] $Origin = '', [int] $Expected = 200, [hashtable] $ExtraHeaders = @{}) {
    $headers = @{} + $ExtraHeaders
    if ($Jwt) { $headers.Authorization = 'Bearer ' + $Jwt }
    if ($Origin) { $headers.Origin = $Origin }
    $arguments = @{
        Uri = $script:apiBase + $Path
        Method = $Method
        Headers = $headers
        TimeoutSec = 15
        SkipHttpErrorCheck = $true
    }
    if ($null -ne $Body) {
        $arguments.ContentType = 'application/json'
        $arguments.Body = $Body | ConvertTo-Json -Depth 12 -Compress
    }
    $response = Invoke-WebRequest @arguments
    Assert-That ($response.StatusCode -eq $Expected) "$stage returned HTTP $($response.StatusCode), expected $Expected."
    return $response
}

function Get-MailToken([string] $Page) {
    $query = [uri]::EscapeDataString('to:' + $email)
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        $results = Invoke-RestMethod -Uri "$script:mailBase/api/v1/search?query=$query" -TimeoutSec 5
        foreach ($summary in $results.messages) {
            $message = Invoke-RestMethod -Uri "$script:mailBase/api/v1/message/$($summary.ID)" -TimeoutSec 5
            $recipients = @($message.To | ForEach-Object { $_.Address })
            Assert-That ($recipients -contains $email) 'Unexpected disposable-mailbox recipient.'
            $match = [regex]::Match($message.Text, 'https://[^\s<>]+/' + $Page + '\?token=([A-Za-z0-9_-]{43})')
            if ($match.Success) {
                $link = [uri]$match.Value
                Assert-That ($link.GetLeftPart([System.UriPartial]::Authority) -eq $frontend) 'Mail points to the wrong frontend.'
                return $match.Groups[1].Value
            }
        }
        Start-Sleep -Milliseconds 250
    }
    throw 'Expected local SMTP message was not delivered.'
}

try {
    docker info --format '{{.ServerVersion}}' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Docker must be running.' }

    $stage = 'production image build'
    docker build --quiet --tag $image (Join-Path $repository 'backend') | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not build the disposable backend image.' }
    $imageCreated = $true

    docker network create $network | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the disposable network.' }
    $networkCreated = $true
    docker run --detach --rm --name $database --network $network `
        --env POSTGRES_USER=deployment --env "POSTGRES_PASSWORD=$databasePassword" --env POSTGRES_DB=deployment `
        postgres:16-alpine | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not start the disposable database.' }
    $createdContainers.Add($database)
    $ready = $false
    for ($attempt = 0; $attempt -lt 40; $attempt++) {
        docker exec $database pg_isready -h 127.0.0.1 -U deployment -d deployment 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    Assert-That $ready 'Disposable PostgreSQL did not become ready.'

    docker run --detach --rm --name $mailpit --network $network --publish '127.0.0.1::8025' `
        axllent/mailpit:v1.27 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not start the disposable SMTP mailbox.' }
    $createdContainers.Add($mailpit)
    $script:mailBase = 'http://127.0.0.1:' + (Get-PublishedPort $mailpit '8025/tcp')

    $stage = 'production profile startup and custom PORT'
    # Plaintext SMTP/JDBC are deliberate test-only overrides inside this disposable network.
    # No real Supabase, Vercel, SMTP service, account or deployment is contacted by this test.
    docker run --detach --rm --name $backend --network $network --publish '127.0.0.1::8097' `
        --env SPRING_PROFILES_ACTIVE=prod --env PORT=8097 `
        --env "SPRING_DATASOURCE_URL=jdbc:postgresql://${database}:5432/deployment?currentSchema=training_app&prepareThreshold=0" `
        --env SPRING_DATASOURCE_USERNAME=deployment --env "SPRING_DATASOURCE_PASSWORD=$databasePassword" `
        --env "SPRING_FLYWAY_URL=jdbc:postgresql://${database}:5432/deployment?currentSchema=training_app" `
        --env SPRING_FLYWAY_USER=deployment --env "SPRING_FLYWAY_PASSWORD=$databasePassword" `
        --env "APP_JWT_SECRET=$jwtSecret" --env "APP_FRONTEND_URL=$frontend" `
        --env "APP_CORS_ALLOWED_ORIGINS=$preview" --env APP_MAIL_FROM=no-reply@example.test `
        --env "SPRING_MAIL_HOST=$mailpit" --env SPRING_MAIL_PORT=1025 `
        --env SPRING_MAIL_USERNAME=local-only --env SPRING_MAIL_PASSWORD=local-only `
        --env SMTP_AUTH=false --env SMTP_STARTTLS_ENABLE=false --env SMTP_SSL_ENABLE=false `
        $image | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not start the disposable backend.' }
    $createdContainers.Add($backend)
    $script:apiBase = 'http://127.0.0.1:' + (Get-PublishedPort $backend '8097/tcp')
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        try {
            $health = Invoke-Api '/api/health'
            if (($health.Content | ConvertFrom-Json).status -eq 'UP') { $ready = $true; break }
        } catch { }
        Start-Sleep -Seconds 1
    }
    Assert-That $ready 'Production backend did not become ready on the configured PORT.'
    Write-Output 'PASS: prod profile starts on custom PORT 8097.'

    $stage = 'private schema and migrations'
    Assert-That ((Invoke-Sql 'SELECT count(*) FROM training_app.flyway_schema_history WHERE success AND version IS NOT NULL;') -eq '8') 'Expected all eight migrations.'
    Assert-That ((Invoke-Sql 'SELECT count(*) FROM training_app.exercises;') -eq '48') 'Expected 48 catalog exercises.'
    Assert-That ((Invoke-Sql "SELECT count(*) FROM training_app.users WHERE email = 'dev@example.test';") -eq '0') 'Production created a development account.'
    Assert-That ((Invoke-Sql "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('users','verification_tokens','exercises','workout_plans','plan_days','plan_exercises');") -eq '0') 'Application tables leaked into public.'
    Write-Output 'PASS: Flyway V1-V8 and 48 exercises in training_app; no development account or public application tables.'

    $stage = 'exact production CORS'
    foreach ($origin in @($frontend, $preview)) {
        $response = Invoke-Api '/api/health' -Origin $origin
        Assert-That (($response.Headers['Access-Control-Allow-Origin'] -join ',') -eq $origin) 'Allowed origin was not returned exactly.'
        $response = Invoke-Api '/api/auth/login' -Method OPTIONS -Origin $origin -ExtraHeaders @{
            'Access-Control-Request-Method' = 'POST'
            'Access-Control-Request-Headers' = 'authorization,content-type'
        }
        Assert-That (($response.Headers['Access-Control-Allow-Origin'] -join ',') -eq $origin) 'Preflight did not allow the configured origin.'
        Assert-That (($response.Headers['Access-Control-Allow-Headers'] -join ',').ToLowerInvariant().Contains('authorization')) 'Preflight omitted Authorization.'
    }
    foreach ($origin in @('https://unrelated.vercel.app', 'http://localhost:3000')) {
        Invoke-Api '/api/health' -Origin $origin -Expected 403 | Out-Null
    }
    Write-Output 'PASS: exact frontend/preview CORS and preflight; unrelated Vercel tenants and localhost rejected.'

    $stage = 'protected API and disabled Swagger'
    Invoke-Api '/api/users/me' -Expected 401 | Out-Null
    foreach ($path in @('/api-docs', '/swagger-ui', '/swagger-ui/index.html')) {
        $response = Invoke-WebRequest -Uri ($script:apiBase + $path) -TimeoutSec 10 -SkipHttpErrorCheck
        Assert-That ($response.StatusCode -ge 400) 'Swagger documentation is public in prod.'
    }
    Write-Output 'PASS: authentication required and Swagger unavailable.'

    $stage = 'registration and real local SMTP delivery'
    Invoke-Api '/api/auth/register' -Method POST -Origin $frontend -Expected 201 -Body @{
        email = $email; password = $password; displayName = 'Production profile smoke'
    } | Out-Null
    $confirmation = Get-MailToken 'confirm-email'
    Invoke-Api '/api/auth/confirm' -Method POST -Origin $frontend -Body @{ token = $confirmation } | Out-Null
    $login = Invoke-Api '/api/auth/login' -Method POST -Origin $frontend -Body @{ email = $email; password = $password }
    $jwt = ($login.Content | ConvertFrom-Json).accessToken
    $response = Invoke-Api '/api/exercises' -Jwt $jwt -Origin $frontend
    $exercises = @($response.Content | ConvertFrom-Json)
    Assert-That ($exercises.Count -eq 48) 'Hibernate did not read the private-schema catalog.'
    Write-Output 'PASS: registration, HTTPS frontend mail links, activation, login and catalog through production JPA.'

    $stage = 'private-schema workout plan persistence'
    $plan = Invoke-Api '/api/plans' -Method POST -Jwt $jwt -Origin $frontend -Expected 201 -Body @{
        name = 'Deployment smoke plan'; description = 'Disposable local test'; days = @(@{
            name = 'Day 1'; exercises = @(@{ exerciseId = $exercises[0].id; sets = 3; reps = 10; weight = 12.5 })
        })
    }
    $planId = ($plan.Content | ConvertFrom-Json).id
    Invoke-Api "/api/plans/$planId" -Jwt $jwt -Origin $frontend | Out-Null
    Assert-That ((Invoke-Sql 'SELECT count(*) FROM training_app.workout_plans;') -eq '1') 'Plan was not persisted in training_app.'
    Write-Output 'PASS: workout plan creation and read use training_app.'

    $stage = 'email-confirmed account deletion and cascade'
    Invoke-Api '/api/users/me/deletion-request' -Method POST -Jwt $jwt -Origin $frontend -Body @{ password = $password } | Out-Null
    $deletion = Get-MailToken 'confirm-deletion'
    Invoke-Api '/api/auth/confirm-deletion' -Method POST -Origin $frontend -Body @{ token = $deletion } | Out-Null
    Invoke-Api '/api/users/me' -Jwt $jwt -Origin $frontend -Expected 401 | Out-Null
    Assert-That ((Invoke-Sql 'SELECT count(*) FROM training_app.users;') -eq '0') 'Test account was not removed.'
    Assert-That ((Invoke-Sql 'SELECT count(*) FROM training_app.workout_plans;') -eq '0') 'Account deletion did not cascade to plans.'
    Assert-That ((Invoke-Sql 'SELECT count(*) FROM training_app.verification_tokens;') -eq '0') 'Verification tokens remained after deletion.'
    Write-Output 'PASS: local SMTP deletion confirmation revokes JWT and removes account, tokens and plans.'
    Write-Output 'PASS: isolated production-profile deployment smoke test complete.'
} catch {
    # Never print credentials, JWTs, tokens, mail contents or request/response bodies.
    throw "Deployment smoke failed during: $stage. $($_.Exception.Message)"
} finally {
    # Remove only resources whose random names were successfully created by this invocation.
    for ($index = $createdContainers.Count - 1; $index -ge 0; $index--) {
        docker rm --force $createdContainers[$index] 2>&1 | Out-Null
    }
    if ($networkCreated) { docker network rm $network 2>&1 | Out-Null }
    if ($imageCreated) { docker image rm $image 2>&1 | Out-Null }
}
