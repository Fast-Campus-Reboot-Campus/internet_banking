<#
.SYNOPSIS
    Internet Banking MVP — 전체 서비스 검증 스크립트

.DESCRIPTION
    deposit-service 와 consultation-service 의 핵심 API 를 모두 검증합니다.

.PARAMETER DepositUrl
    deposit-service 기본 URL (기본값: http://localhost:8082/api)

.PARAMETER ConsultUrl
    consultation-service 기본 URL (기본값: http://localhost:8090)

.EXAMPLE
    .\scripts\verify-all.ps1
    .\scripts\verify-all.ps1 -DepositUrl http://localhost:8082/api -ConsultUrl http://localhost:8090
#>
param(
    [string] $DepositUrl = "http://localhost:8082/api",
    [string] $ConsultUrl = "http://localhost:8090"
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$failures = @()

# ── deposit-service ────────────────────────────────────────
Write-Host ""
Write-Host "================================================================"
Write-Host " [1/2] deposit-service 검증  ($DepositUrl)"
Write-Host "================================================================"
try {
    & "$repoRoot\services\deposit-service\scripts\verify.ps1" -BaseUrl $DepositUrl
} catch {
    $failures += "deposit-service: $_"
    Write-Host "  [FAIL] $_" -ForegroundColor Red
}

# ── consultation-service ───────────────────────────────────
Write-Host ""
Write-Host "================================================================"
Write-Host " [2/2] consultation-service 검증  ($ConsultUrl)"
Write-Host "================================================================"
try {
    & "$repoRoot\services\consultation-service\scripts\verify.ps1" -BaseUrl $ConsultUrl
} catch {
    $failures += "consultation-service: $_"
    Write-Host "  [FAIL] $_" -ForegroundColor Red
}

# ── 결과 요약 ──────────────────────────────────────────────
Write-Host ""
Write-Host "================================================================"
if ($failures.Count -eq 0) {
    Write-Host " ALL_SERVICES_OK" -ForegroundColor Green
    Write-Host "================================================================"
} else {
    Write-Host " 검증 실패 항목:" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    Write-Host "================================================================"
    exit 1
}
