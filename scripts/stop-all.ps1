<#
.SYNOPSIS
    Internet Banking MVP — 전체 서비스 중지 스크립트

.DESCRIPTION
    Docker 인프라(deposit-db, redis, kafka)를 중지합니다.
    앱 서비스(deposit-service, consultation-service)는 각 창을 닫거나 Ctrl+C 로 종료하세요.

.PARAMETER RemoveVolumes
    DB 볼륨까지 삭제합니다 (데이터 초기화 시 사용).
    주의: 모든 DB 데이터가 삭제됩니다.

.PARAMETER Service
    특정 서비스만 중지 (예: deposit-db, redis, kafka). 비어있으면 전체 중지.

.EXAMPLE
    # Docker 인프라 전체 중지
    .\scripts\stop-all.ps1

    # 볼륨 포함 완전 초기화 (DB 데이터 삭제)
    .\scripts\stop-all.ps1 -RemoveVolumes

    # kafka 만 중지
    .\scripts\stop-all.ps1 -Service kafka
#>
param(
    [switch] $RemoveVolumes,
    [string] $Service = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$composeFile = "$repoRoot\docker-compose.yml"

if ($RemoveVolumes) {
    Write-Host "경고: DB 볼륨 포함 완전 삭제 (-RemoveVolumes 옵션)" -ForegroundColor Yellow
    $confirm = Read-Host "계속하시겠습니까? 모든 DB 데이터가 삭제됩니다. (y/N)"
    if ($confirm -ne "y" -and $confirm -ne "Y") {
        Write-Host "취소됨."
        exit 0
    }
}

if ($Service) {
    Write-Host "=== $Service 중지 ==="
    docker compose -f $composeFile stop $Service
} else {
    Write-Host "=== Docker 인프라 전체 중지 (deposit-db, redis, kafka) ==="
    docker compose -f $composeFile stop deposit-db redis kafka
}

if ($LASTEXITCODE -ne 0) { throw "docker compose stop 실패" }

if ($RemoveVolumes) {
    Write-Host ""
    Write-Host "=== 볼륨 삭제 중 ==="
    docker compose -f $composeFile down -v
    if ($LASTEXITCODE -ne 0) { throw "docker compose down -v 실패" }
    Write-Host "  -> 볼륨 삭제 완료"
}

Write-Host ""
Write-Host "중지 완료."
Write-Host "앱 서비스(deposit-service, consultation-service)는 각 창에서 Ctrl+C 로 종료하세요."
