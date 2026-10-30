@echo off
title AI Chat Stopper
:: ============ Usage ============
::   Stopper.bat           Stop Electron / Backend(8080) / Frontend(5187) / Redis(6379) / leftover node
::   Stopper.bat all       Also stop MySQL service & Ollama & Prometheus & Grafana
echo ============================================
echo   AI Chat Stopper
echo   Will stop : Desktop(Electron) / Backend(8080) / Frontend(5187) / Redis(6379)
echo   Kept     : MySQL & Ollama (system services)
echo   To also stop them, run with argument:  all
echo ============================================
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\stop-all.ps1" %*
echo.
echo Press any key to close this window...
pause >nul
