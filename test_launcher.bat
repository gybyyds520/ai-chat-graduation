@echo off
chcp 65001 >nul
title AI Chat Launcher Test

echo ==========================================
echo        Testing Option 1: Full Start
echo ==========================================
echo.

set "LOG_FILE=launcher_test.log"

echo [TEST] Starting launcher with option 1...
echo [%date% %time%] Starting launcher test >> "%LOG_FILE%"

:: 模拟选择选项1
(
    echo 1
    timeout /t 15 /nobreak >nul
) | launcher.bat

echo [%date% %time%] Launcher test completed >> "%LOG_FILE%"
echo.
echo [TEST] Test completed. Check launcher.log for details.
echo.
pause