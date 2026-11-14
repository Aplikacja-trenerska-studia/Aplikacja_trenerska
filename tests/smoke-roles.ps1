$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot
$container = 'trainer-role-test-' + [guid]::NewGuid().ToString('N').Substring(0, 12)

function Invoke-TestSql([string] $Sql) {
    $output = $Sql | docker exec -i $container psql -X -U postgres -d postgres -v ON_ERROR_STOP=1 -At
    if ($LASTEXITCODE -ne 0) { throw 'Test SQL failed.' }
    return ($output -join "`n").Trim()
}

function Set-TestRole([string] $Email, [string] $Role, [bool] $ShouldSucceed = $true) {
    $output = Get-Content -LiteralPath (Join-Path $repository 'tools/set-user-role.sql') -Raw |
        docker exec -i $container psql -X -U postgres -d postgres -v ON_ERROR_STOP=1 -v "email=$Email" -v "role=$Role" -f - 2>&1
    $resultCode = $LASTEXITCODE
    if (($resultCode -eq 0) -ne $ShouldSucceed) {
        throw "Unexpected exit code $resultCode for role=$Role email=$Email : $output"
    }
}

function Assert-AccountState([string] $Expected) {
    $actual = Invoke-TestSql "SELECT role || ':' || token_version FROM users WHERE email = 'athlete@example.com';"
    if ($actual -ne $Expected) { throw "Expected $Expected, got $actual" }
}

try {
    docker run --detach --rm --name $container -e POSTGRES_HOST_AUTH_METHOD=trust postgres:16-alpine | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not start disposable PostgreSQL.' }
    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        docker exec $container pg_isready -U postgres 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'PostgreSQL did not become ready.' }

    foreach ($migration in @('V1__users_and_verification_tokens.sql', 'V2__account_security.sql')) {
        Invoke-TestSql (Get-Content -LiteralPath (Join-Path $repository "backend/src/main/resources/db/migration/$migration") -Raw) | Out-Null
    }
    Invoke-TestSql @'
INSERT INTO users (email, password_hash, display_name, role, enabled) VALUES
('athlete@example.com', 'unused', 'Test athlete', 'USER', TRUE),
('inactive@example.com', 'unused', 'Inactive', 'USER', FALSE),
('admin@example.com', 'unused', 'Admin', 'ADMIN', TRUE);
'@ | Out-Null

    Set-TestRole ' ATHLETE@EXAMPLE.COM ' 'TRAINER'
    Assert-AccountState 'TRAINER:1'
    Set-TestRole 'athlete@example.com' 'TRAINER'
    Assert-AccountState 'TRAINER:1' # Repeating the operation does not log the account out again.
    Set-TestRole 'athlete@example.com' 'USER'
    Assert-AccountState 'USER:2'

    Set-TestRole 'athlete@example.com' 'ADMIN' $false
    Set-TestRole 'athlete@example.com' "TRAINER'; DELETE FROM users; --" $false
    Set-TestRole "x' OR true --" 'TRAINER' $false
    Set-TestRole 'missing@example.com' 'TRAINER' $false
    Set-TestRole 'inactive@example.com' 'TRAINER' $false
    Set-TestRole 'admin@example.com' 'TRAINER' $false
    Assert-AccountState 'USER:2'
    $otherAccounts = Invoke-TestSql "SELECT string_agg(email || ':' || role || ':' || token_version, ',' ORDER BY email) FROM users WHERE email <> 'athlete@example.com';"
    if ($otherAccounts -ne 'admin@example.com:ADMIN:0,inactive@example.com:USER:0') {
        throw "Unexpected change to other accounts: $otherAccounts"
    }
    Write-Output 'PASS: promotion, revocation, session versions, idempotency, invalid roles, inactive/missing accounts and SQL injection.'
}
finally {
    docker stop $container 2>&1 | Out-Null
}
