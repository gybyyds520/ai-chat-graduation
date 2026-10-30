# AI Chat Web Server Deployment Script
# Deploys the frontend to a local web server

@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "DIST_DIR=%SCRIPT_DIR%..\dist"
set "PORT=8080"

echo ========================================
echo    AI Chat Web Deployment
echo ========================================
echo.

:: Check if dist directory exists
if not exist "%DIST_DIR%" (
    echo ERROR: dist directory not found!
    echo Please run 'npm run build' first.
    pause
    exit /b 1
)

echo [1/3] Checking Python availability...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Python not found in PATH
    pause
    exit /b 1
)

echo [2/3] Starting HTTP server on port %PORT%...
echo.
echo Serving from: %DIST_DIR%
echo Access URL: http://localhost:%PORT%
echo.
echo Press Ctrl+C to stop the server
echo.

:: Start Python HTTP server
cd /d "%DIST_DIR%"
python -m http.server %PORT%

endlocal