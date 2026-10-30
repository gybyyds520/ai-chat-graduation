@echo off
title AI Chat Launcher
:: ============ Usage ============
::   Launcher.bat          Start all services (MySQL/Redis/Ollama/Backend/Frontend/Desktop)
::   Launcher.bat monitor  Start all services + Prometheus monitoring
::   Launcher.bat open     Open browser only (when project already running)
::   Launcher.bat elevated Admin instance (internal: add firewall rules)

if "%~1"=="elevated" goto :elevated
if "%~1"=="open" goto :open
if "%~1"=="monitor" goto :monitor

:: ---------- Already-running check: open browser directly ----------
powershell -NoProfile -Command "$api=[bool](Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue); $web=[bool](Get-NetTCPConnection -State Listen -LocalPort 5187 -ErrorAction SilentlyContinue); if($api -and $web){ exit 0 } else { exit 1 }" >nul 2>&1
if %errorlevel%==0 (
    echo ============================================
    echo   AI Chat is already running, opening browser...
    echo   If you want a full restart, run the stop script first.
    echo ============================================
    echo.
    start http://localhost:5187
    echo.
    pause >nul
    exit /b
)

:: ---------- Firewall rule (mobile access; failure never blocks startup) ----------
netsh advfirewall firewall show rule name="AI Chat Web 5187" >nul 2>&1
if errorlevel 1 (
    echo [INFO] Requesting admin to add firewall rules (needed for mobile access)
    echo        If the dialog is ignored or declined, PC use still works fine.
    powershell -NoProfile -Command "Start-Process '%~f0' -Verb RunAs -ArgumentList 'elevated'" 2>nul
    timeout /t 3 >nul
)

echo ============================================
echo   AI Chat Launcher - starting all services...
echo   MySQL / Redis / Ollama / Backend(8080) / Frontend(5187) / Desktop
echo   启动完成后自动执行健康自检（后端 / Redis / 桌面窗口）
echo ============================================
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-all.ps1"
echo.
pause >nul
exit /b

:open
start http://localhost:5187
exit /b

:monitor
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-all.ps1" -Monitor
echo.
pause >nul
exit /b

:elevated
:: Admin instance: add firewall rules (idempotent)
netsh advfirewall firewall show rule name="AI Chat Web 5187" >nul 2>&1
if errorlevel 1 (
    netsh advfirewall firewall add rule name="AI Chat Web 5187" dir=in action=allow protocol=TCP localport=5187 >nul
    netsh advfirewall firewall add rule name="AI Chat API 8080" dir=in action=allow protocol=TCP localport=8080 >nul
)
exit /b
