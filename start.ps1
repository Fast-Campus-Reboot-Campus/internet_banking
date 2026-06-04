$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " Internet Banking - Start Local Dev Servers" -ForegroundColor Cyan
Write-Host "============================================================"
Write-Host ""

# Docker check
docker info 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Docker Desktop is not running." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Set env vars for bootRun (inherited by child processes)
$env:SPRING_PROFILES_ACTIVE   = "local"
$env:CUSTOMER_DB_PORT         = "15432"
$env:DEPOSIT_DB_PORT          = "5433"
$env:DEPOSIT_DB_PASSWORD      = "deposit"
$env:LOAN_DB_PORT             = "5434"
$env:PAYMENT_DB_PORT          = "5435"
$env:MASTER_DB_PORT           = "5436"
$env:COMMON_DB_PORT           = "5438"
$env:REDIS_HOST               = "localhost"
$env:SCHEMA_REGISTRY_URL      = "http://localhost:18081"
# payment-service: 통합 단일 브로커(ib-kafka:9092)로 kftc/bok/internal 3 논리 클러스터 통합
$env:KFTC_KAFKA_BOOTSTRAP     = "localhost:9092"
$env:BOK_KAFKA_BOOTSTRAP      = "localhost:9092"
$env:INTERNAL_KAFKA_BOOTSTRAP = "localhost:9092"
$env:CRYPTO_KEY_BASE64        = "OhIN2XxxrFW6nzY6iIQ8sTfTr/L6NQJxgdCZfqJon+k="
$env:LANGFUSE_PORT            = "3002"

# Load secrets (OPENAI_API_KEY etc.) from .env
$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '^[A-Za-z_][A-Za-z_0-9]*=' } | ForEach-Object {
        $idx = $_.IndexOf('=')
        $key = $_.Substring(0, $idx)
        $val = $_.Substring($idx + 1)
        [System.Environment]::SetEnvironmentVariable($key, $val, 'Process')
    }
}

# 1. Start infra containers only (skip Docker-build app services)
Write-Host "[1/3] Starting infrastructure..." -ForegroundColor Yellow
$infra = @(
    "customer-db","deposit-db","loan-db","common-db","payment-db","master-db","ai-db",
    "kafka","schema-registry","redis",
    "prometheus","grafana",
    "loki","promtail","langfuse-db","langfuse","phoenix"
)
docker compose up -d @infra
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] docker compose failed" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# 2. Wait for health
Write-Host ""
Write-Host "[2/3] Waiting for health checks (up to 90s)..." -ForegroundColor Yellow
$waited = 0
while ($waited -lt 90) {
    Start-Sleep -Seconds 5
    $waited += 5
    $starting = docker compose ps --format "{{.Health}}" 2>$null | Where-Object { $_ -eq "starting" }
    if (-not $starting) { break }
    Write-Host "  waiting... ($waited`s)"
}
Write-Host "  Infrastructure ready ($waited`s)" -ForegroundColor Green

# 3. Start Spring Boot services + web
Write-Host ""
Write-Host "[3/3] Starting services..." -ForegroundColor Yellow

Start-Process cmd -ArgumentList "/k gradlew.bat :services:customer-service:bootRun" -WorkingDirectory $root -WindowStyle Normal
Start-Sleep -Seconds 2
Start-Process cmd -ArgumentList "/k gradlew.bat :services:deposit-service:bootRun" -WorkingDirectory $root -WindowStyle Normal
Start-Sleep -Seconds 2
Start-Process cmd -ArgumentList "/k gradlew.bat :services:loan-service:bootRun" -WorkingDirectory $root -WindowStyle Normal
Start-Sleep -Seconds 2
Start-Process cmd -ArgumentList "/k gradlew.bat :services:payment-service:bootRun" -WorkingDirectory $root -WindowStyle Normal
Start-Sleep -Seconds 2
Start-Process cmd -ArgumentList "/k gradlew.bat :services:master-service:bootRun" -WorkingDirectory $root -WindowStyle Normal
Start-Sleep -Seconds 2
Start-Process cmd -ArgumentList "/k npm run dev" -WorkingDirectory "$root\web" -WindowStyle Normal

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " 6 service windows opened (ready in ~30-60s)"
Write-Host ""
Write-Host " Web UI      http://localhost:3001" -ForegroundColor Green
Write-Host ""
Write-Host " Swagger:"
Write-Host "   customer  http://localhost:8081/swagger-ui.html"
Write-Host "   deposit   http://localhost:8082/swagger-ui.html"
Write-Host "   loan      http://localhost:8083/swagger-ui.html"
Write-Host "   payment   http://localhost:8084/swagger-ui.html"
Write-Host "   master    http://localhost:8085/swagger-ui.html"
Write-Host ""
Write-Host " Grafana     http://localhost:3000  (admin/admin)" -ForegroundColor Green
Write-Host " Langfuse    http://localhost:3002" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Read-Host "Press Enter to exit"
