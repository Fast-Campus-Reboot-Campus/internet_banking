# AX풀뱅크 전체 서비스 시작 스크립트
# 실행: PowerShell에서 .\start-all.ps1

$root = $PSScriptRoot

Write-Host "=== AX풀뱅크 서비스 시작 ===" -ForegroundColor Cyan

# 1. Consultation Service (Python)
Write-Host "[1/3] Consultation Service (8087) 시작..." -ForegroundColor Yellow
$consultDir = Join-Path $root "services\consultation-service"
Start-Process powershell -ArgumentList "-NoProfile -Command `"cd '$consultDir'; python -m uvicorn app.main:app --host 0.0.0.0 --port 8087 --reload`"" -WindowStyle Normal

Start-Sleep -Seconds 2

# 2. Deposit Service (Spring Boot)
Write-Host "[2/3] Deposit Service (8082) 시작..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoProfile -Command `"cd '$root'; .\gradlew.bat :services:deposit-service:bootRun`"" -WindowStyle Normal

Start-Sleep -Seconds 2

# 3. Web (Next.js)
Write-Host "[3/3] Web (3001) 시작..." -ForegroundColor Yellow
$webDir = Join-Path $root "web"
Start-Process powershell -ArgumentList "-NoProfile -Command `"cd '$webDir'; npm run dev`"" -WindowStyle Normal

Write-Host ""
Write-Host "서비스 시작 완료 (기동까지 약 30초 소요)" -ForegroundColor Green
Write-Host "  Web:          http://localhost:3001" -ForegroundColor White
Write-Host "  Deposit API:  http://localhost:8082" -ForegroundColor White
Write-Host "  Consultation: http://localhost:8087" -ForegroundColor White
