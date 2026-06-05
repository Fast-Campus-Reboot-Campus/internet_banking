@echo off
setlocal enabledelayedexpansion
set ROOT=%~dp0
cd /d "%ROOT%"

echo ============================================================
echo  Internet Banking - Initial Setup
echo ============================================================
echo.

echo [1/4] Checking prerequisites...

where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker not found. Install Docker Desktop first.
    goto :END
)

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Desktop is not running. Please start it first.
    goto :END
)

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Install JDK 17 or higher.
    goto :END
)

where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found. Install Node.js 18 or higher.
    goto :END
)

echo   OK: Docker / Java / Node.js

echo.
echo [2/4] Setting up .env ...
if not exist "%ROOT%.env" (
    copy "%ROOT%.env.sample" "%ROOT%.env" > nul
    echo   Copied .env.sample to .env
    powershell -Command "(Get-Content '%ROOT%.env') -replace 'REDIS_HOST=redis','REDIS_HOST=localhost' | Set-Content '%ROOT%.env'"
    echo   Set REDIS_HOST=localhost for local bootRun
) else (
    echo   .env already exists - skipped
)

echo.
echo [3/4] Creating log directory...
if not exist "C:\logs\internet-banking" (
    mkdir "C:\logs\internet-banking"
    echo   Created C:\logs\internet-banking
) else (
    echo   Log directory already exists - skipped
)

echo.
echo [4/4] Installing web dependencies...
if not exist "%ROOT%web\node_modules" (
    cd /d "%ROOT%web"
    call npm install
    if %errorlevel% neq 0 (
        echo [ERROR] npm install failed
        cd /d "%ROOT%"
        goto :END
    )
    cd /d "%ROOT%"
    echo   npm install done
) else (
    echo   node_modules already exists - skipped
)

echo.
echo ============================================================
echo  Setup complete! Run start.bat to launch services.
echo ============================================================

:END
echo.
pause
