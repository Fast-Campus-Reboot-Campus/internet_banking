<#
.SYNOPSIS
    Internet Banking MVP — 최초 설치 / 초기화 스크립트

.DESCRIPTION
    처음 한 번만 실행하면 됩니다.
    1. consultation-service Python venv 생성 + 패키지 설치
    2. deposit-service DB 스키마 초기화 (V1 + V5 + V6)
    3. deposit-service 데모 데이터 적재 (V2)
    4. consultation-service DB 스키마 초기화 (ddl.sql)
    5. consultation-service 데모 데이터 적재 (demo-data.sql)

.PARAMETER DbHost
    PostgreSQL 호스트 (기본값: localhost)

.PARAMETER DbPort
    PostgreSQL 포트 (기본값: 5432 — 로컬 직접 연결)

.PARAMETER DbUser
    PostgreSQL 사용자 (기본값: deposit)

.PARAMETER PsqlPath
    psql 실행 파일 경로

.PARAMETER PythonExe
    Python 실행 파일명 또는 경로 (기본값: python)

.EXAMPLE
    # 로컬 PostgreSQL (5432)
    .\scripts\setup-all.ps1

    # Docker deposit-db (5433 포트, postgres 유저)
    .\scripts\setup-all.ps1 -DbPort 5433 -DbUser postgres
#>
param(
    [string] $DbHost    = "localhost",
    [int]    $DbPort    = 5432,
    [string] $DbUser    = "deposit",
    [string] $PsqlPath  = "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    [string] $PythonExe = "python"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

function Step {
    param([int]$n, [int]$total, [string]$title)
    Write-Host ""
    Write-Host "================================================================"
    Write-Host " [$n/$total] $title"
    Write-Host "================================================================"
}

# deposit / consultation 스크립트가 공통으로 사용하는 psql 파라미터
# 파라미터명 -HostName, -Port, -User, -PsqlPath 는 두 서비스 스크립트 모두 동일
$psqlCommon = @{
    HostName = $DbHost
    Port     = $DbPort
    User     = $DbUser
    PsqlPath = $PsqlPath
}

# ── 1. Python venv ────────────────────────────────────────
Step 1 5 "consultation-service Python venv 설정"
& "$repoRoot\services\consultation-service\scripts\setup-venv.ps1" -PythonExe $PythonExe
if ($LASTEXITCODE -ne 0) { throw "venv 설정 실패" }

# ── 2. deposit-service DB 스키마 ──────────────────────────
Step 2 5 "deposit-service DB 스키마 초기화 (V1 + V5 + V6)"
& "$repoRoot\services\deposit-service\scripts\init-db.ps1" @psqlCommon
if ($LASTEXITCODE -ne 0) { throw "deposit 스키마 초기화 실패" }

# ── 3. deposit-service 데모 데이터 ────────────────────────
Step 3 5 "deposit-service 데모 데이터 적재 (V2)"
& "$repoRoot\services\deposit-service\scripts\seed-demo-data.ps1" @psqlCommon
if ($LASTEXITCODE -ne 0) { throw "deposit 데모 데이터 적재 실패" }

# ── 4. consultation-service DB 스키마 ─────────────────────
Step 4 5 "consultation-service DB 스키마 초기화 (ddl.sql)"
& "$repoRoot\services\consultation-service\scripts\init-db.ps1" `
    -Database "deposit_db" @psqlCommon
if ($LASTEXITCODE -ne 0) { throw "consultation 스키마 초기화 실패" }

# ── 5. consultation-service 데모 데이터 ───────────────────
Step 5 5 "consultation-service 데모 데이터 적재 (demo-data.sql)"
& "$repoRoot\services\consultation-service\scripts\seed-demo-data.ps1" `
    -Database "deposit_db" @psqlCommon
if ($LASTEXITCODE -ne 0) { throw "consultation 데모 데이터 적재 실패" }

# ── 완료 ─────────────────────────────────────────────────
Write-Host ""
Write-Host "================================================================"
Write-Host " 초기화 완료! 다음 명령으로 서비스를 시작하세요:"
Write-Host ""
Write-Host "   .\scripts\start-all.ps1"
Write-Host "================================================================"
