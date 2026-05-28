<#
.SYNOPSIS
    Internet Banking MVP — 전체 서비스 시작 스크립트

.DESCRIPTION
    1. Docker 인프라 (deposit-db, redis, kafka) 기동
    2. deposit-service    시작 (새 PowerShell 창, http://localhost:8082/api/)
    3. consultation-service 시작 (새 PowerShell 창, http://localhost:8087)

.PARAMETER DockerOnly
    Docker 서비스만 시작하고 앱 서비스는 시작하지 않음

.PARAMETER SkipDocker
    Docker 기동을 건너뜀 (이미 실행 중인 경우)

.PARAMETER DepositUseFlyway
    deposit-service 를 Flyway 활성 모드로 시작 (빈 DB 에서 처음 시작할 때)

.PARAMETER KafkaEnabled
    consultation-service 에서 Kafka 발행 활성화

.EXAMPLE
    # 처음 시작 (빈 DB, Flyway 자동 마이그레이션)
    .\scripts\start-all.ps1 -DepositUseFlyway

    # init-db + seed 후 시작 (Flyway 비활성)
    .\scripts\start-all.ps1

    # Docker 만 띄울 때
    .\scripts\start-all.ps1 -DockerOnly
#>
param(
    [switch] $DockerOnly,
    [switch] $SkipDocker,
    [switch] $DepositUseFlyway,
    [switch] $KafkaEnabled
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

# ── 1. Docker 인프라 ──────────────────────────────────────
if (-not $SkipDocker) {
    Write-Host "=== [1/3] Docker 인프라 시작 (deposit-db, redis, kafka) ==="
    docker compose -f "$repoRoot\docker-compose.yml" up -d deposit-db redis kafka
    if ($LASTEXITCODE -ne 0) { throw "Docker 서비스 시작 실패" }

    Write-Host "  DB/Redis/Kafka 준비 대기 (10초) ..."
    Start-Sleep -Seconds 10
    Write-Host "  -> Docker OK"
} else {
    Write-Host "=== [1/3] Docker 기동 건너뜀 (--SkipDocker) ==="
}

if ($DockerOnly) {
    Write-Host ""
    Write-Host "Docker 전용 모드 종료. 앱 서비스는 개별 start.ps1 로 시작하세요."
    exit 0
}

# ── 2. deposit-service (새 창) ────────────────────────────
Write-Host ""
Write-Host "=== [2/3] deposit-service 시작 (새 창) ==="
$depositScript = Join-Path $repoRoot "services\deposit-service\scripts\start.ps1"

$depositArgs = "-NoExit -File `"$depositScript`""
if ($DepositUseFlyway) { $depositArgs += " -UseFlyway" }

Start-Process powershell -ArgumentList $depositArgs -WindowStyle Normal
Write-Host "  -> deposit-service 시작됨"
Write-Host "     http://localhost:8082/api/"

Start-Sleep -Seconds 5

# ── 3. consultation-service (새 창) ──────────────────────
Write-Host ""
Write-Host "=== [3/3] consultation-service 시작 (새 창) ==="
$consultScript = Join-Path $repoRoot "services\consultation-service\scripts\start.ps1"

$consultArgs = "-NoExit -File `"$consultScript`""
if ($KafkaEnabled) { $consultArgs += " -KafkaEnabled" }

Start-Process powershell -ArgumentList $consultArgs -WindowStyle Normal
Write-Host "  -> consultation-service 시작됨"
Write-Host "     http://localhost:8087"
Write-Host "     http://localhost:8087/chat  (챗봇 UI)"

Write-Host ""
Write-Host "============================================================"
Write-Host " 모든 서비스 시작 완료 (각 창에서 기동 로그 확인)"
Write-Host ""
Write-Host "  deposit-service     : http://localhost:8082/api/"
Write-Host "  consultation-service: http://localhost:8087"
Write-Host "  챗봇 UI             : http://localhost:8087/chat"
Write-Host ""
Write-Host " 검증: .\scripts\verify-all.ps1"
Write-Host "============================================================"
