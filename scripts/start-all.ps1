<#
.SYNOPSIS
    Starts local Internet Banking MVP services.

.DESCRIPTION
    1. Starts Docker infrastructure: customer-db, deposit-db, redis, kafka.
    2. Starts customer-service at http://localhost:8081.
    3. Starts api-gateway at http://localhost:8080.
    4. Starts deposit-service at http://localhost:8082/api/.
    5. Starts consultation-service at http://localhost:8087.

.PARAMETER DockerOnly
    Starts only Docker infrastructure.

.PARAMETER SkipDocker
    Skips Docker infrastructure startup.

.PARAMETER DepositUseFlyway
    Starts deposit-service in Flyway mode.

.PARAMETER KafkaEnabled
    Enables Kafka publishing in consultation-service.
#>
param(
    [switch] $DockerOnly,
    [switch] $SkipDocker,
    [switch] $DepositUseFlyway,
    [switch] $KafkaEnabled
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not $SkipDocker) {
    Write-Host "=== [1/5] Starting Docker infra (customer-db, deposit-db, redis, kafka) ==="
    docker compose -f "$repoRoot\docker-compose.yml" up -d customer-db deposit-db redis kafka
    if ($LASTEXITCODE -ne 0) { throw "Docker service startup failed" }

    Write-Host "  Waiting for DB/Redis/Kafka readiness (10s)..."
    Start-Sleep -Seconds 10
    Write-Host "  -> Docker OK"
} else {
    Write-Host "=== [1/5] Skipping Docker startup (--SkipDocker) ==="
}

if ($DockerOnly) {
    Write-Host ""
    Write-Host "Docker-only mode complete."
    exit 0
}

Write-Host ""
Write-Host "=== [2/5] Starting customer-service ==="
Start-Process powershell -ArgumentList "-NoExit -Command `"cd '$repoRoot'; .\gradlew.bat :services:customer-service:bootRun`"" -WindowStyle Normal
Write-Host "  -> customer-service: http://localhost:8081"

Start-Sleep -Seconds 5

Write-Host ""
Write-Host "=== [3/5] Starting api-gateway ==="
Start-Process powershell -ArgumentList "-NoExit -Command `"cd '$repoRoot'; .\gradlew.bat :services:api-gateway:bootRun`"" -WindowStyle Normal
Write-Host "  -> api-gateway: http://localhost:8080"

Start-Sleep -Seconds 5

Write-Host ""
Write-Host "=== [4/5] Starting deposit-service ==="
$depositScript = Join-Path $repoRoot "services\deposit-service\scripts\start.ps1"
$depositArgs = "-NoExit -File `"$depositScript`""
if ($DepositUseFlyway) { $depositArgs += " -UseFlyway" }

Start-Process powershell -ArgumentList $depositArgs -WindowStyle Normal
Write-Host "  -> deposit-service: http://localhost:8082/api/"

Start-Sleep -Seconds 5

Write-Host ""
Write-Host "=== [5/5] Starting consultation-service ==="
$consultScript = Join-Path $repoRoot "services\consultation-service\scripts\start.ps1"
$consultArgs = "-NoExit -File `"$consultScript`""
if ($KafkaEnabled) { $consultArgs += " -KafkaEnabled" }

Start-Process powershell -ArgumentList $consultArgs -WindowStyle Normal
Write-Host "  -> consultation-service: http://localhost:8087"
Write-Host "  -> chat UI: http://localhost:8087/chat"

Write-Host ""
Write-Host "============================================================"
Write-Host " All services are starting. Check each window for boot logs."
Write-Host ""
Write-Host "  api-gateway         : http://localhost:8080"
Write-Host "  customer-service    : http://localhost:8081"
Write-Host "  deposit-service     : http://localhost:8082/api/"
Write-Host "  consultation-service: http://localhost:8087"
Write-Host "  chat UI             : http://localhost:8087/chat"
Write-Host ""
Write-Host " Verify: .\scripts\verify-all.ps1"
Write-Host "============================================================"
