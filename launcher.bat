@echo off
title AI Chat Launcher

set "LOG_FILE=%~dp0launcher.log"
set "TIMESTAMP="

:main
cls
echo.
echo ==========================================
echo        AI Chat Application Launcher
echo ==========================================
echo.
echo   [1] Start Full Application (Frontend + Backend)
echo   [2] Start Frontend Only
echo   [3] Start Backend Only
echo   [4] Open Browser
echo   [5] Check Status
echo   [6] Stop All Services
echo   [7] Open Mini Program Interface
echo   [0] Exit
echo.
echo ==========================================
echo.

set "choice="
set /p choice="Select option (0-6): "

:: Remove any trailing whitespace/newlines
for /f "tokens=* delims= " %%a in ("%choice%") do set "choice=%%a"

if "%choice%"=="1" (
    call :log "User selected option 1: Start Full Application"
    goto start_all
)
if "%choice%"=="2" (
    call :log "User selected option 2: Start Frontend Only"
    goto start_frontend
)
if "%choice%"=="3" (
    call :log "User selected option 3: Start Backend Only"
    goto start_backend
)
if "%choice%"=="4" (
    call :log "User selected option 4: Open Browser"
    goto open_browser
)
if "%choice%"=="5" (
    call :log "User selected option 5: Check Status"
    goto check_status
)
if "%choice%"=="6" (
    call :log "User selected option 6: Stop All Services"
    goto stop_all
)
if "%choice%"=="7" (
    call :log "User selected option 7: Open Mini Program Interface"
    goto open_miniprogram
)
if "%choice%"=="0" (
    call :log "User selected option 0: Exit"
    goto exit
)
call :log "Invalid selection: %choice%"
goto main

:start_all
cls
call :log "=== Starting Full Application ==="
echo.
echo ==========================================
echo         Starting Full Application
echo ==========================================
echo.

call :log "Step 1/4: Stopping existing services first"
echo [INFO] Step 1/4: Stopping existing services if running...
call :stop_services_silent
echo [DONE] Existing services stopped

call :log "Step 2/4: Starting backend service"
echo.
echo [INFO] Step 2/4: Starting backend service...
echo [DEBUG] Backend path: %~dp0backend\aichat
echo [DEBUG] Command: cd /d "%~dp0backend\aichat" && mvn spring-boot:run

if not exist "%~dp0backend\aichat\pom.xml" (
    call :log "[ERROR] Backend pom.xml not found: %~dp0backend\aichat\pom.xml"
    echo [ERROR] Backend project not found!
    echo [ERROR] Path: %~dp0backend\aichat\pom.xml
    pause
    goto main
)

start "AI Chat - Backend" cmd /k "cd /d %~dp0backend\aichat && mvn spring-boot:run"
call :log "Backend process started in new window"
echo [INFO] Backend process started in new window
echo [WAIT] Waiting 12 seconds for backend to initialize...

for /l %%i in (1,1,12) do (
    echo [PROGRESS] Waiting... %%i/12
    timeout /t 1 /nobreak >nul
    netstat -ano | findstr ":8080" >nul
    if not errorlevel 1 (
        call :log "Backend service detected on port 8080"
        echo [SUCCESS] Backend service started successfully!
        goto :backend_started
    )
)

:backend_started
call :log "Step 3/4: Starting frontend service"
echo.
echo [INFO] Step 3/4: Starting frontend service...
echo [DEBUG] Frontend path: %~dp0frontend
echo [DEBUG] Command: cd /d "%~dp0frontend" && npm run dev

if not exist "%~dp0frontend\package.json" (
    call :log "[ERROR] Frontend package.json not found: %~dp0frontend\package.json"
    echo [ERROR] Frontend project not found!
    echo [ERROR] Path: %~dp0frontend\package.json
    pause
    goto main
)

start "AI Chat - Frontend" cmd /k "cd /d ""%~dp0frontend"" && npm run dev"
call :log "Frontend process started in new window"
echo [INFO] Frontend process started in new window
echo [WAIT] Waiting 8 seconds for frontend to initialize...

for /l %%i in (1,1,8) do (
    echo [PROGRESS] Waiting... %%i/8
    timeout /t 1 /nobreak >nul
    netstat -ano | findstr ":5173" >nul || netstat -ano | findstr ":5174" >nul || netstat -ano | findstr ":5175" >nul || netstat -ano | findstr ":5176" >nul
    if not errorlevel 1 (
        call :log "Frontend service detected"
        echo [SUCCESS] Frontend service started successfully!
        goto :frontend_started
    )
)

:frontend_started
call :log "Step 4/4: Verifying services and opening browser"
echo.
echo [INFO] Step 4/4: Verifying services...
call :check_status_detailed

call :log "All services started successfully"
echo.
echo ==========================================
echo        Application Started Successfully!
echo ==========================================
echo.
echo [SUCCESS] Backend Service: http://localhost:8080
echo [SUCCESS] Frontend Service: http://localhost:5175 (or available port)
echo.
echo [INFO] Opening browser...
start http://localhost:5175
call :log "Browser opened to http://localhost:5175"

echo.
echo ==========================================
echo         Quick Access Links
echo ==========================================
echo.
echo   Frontend:    http://localhost:5175
echo   Backend API: http://localhost:8080
echo   Desktop Elf: http://localhost:5175/#/desktop-elf
echo.
echo ==========================================
echo.
pause
goto main

:start_frontend
cls
call :log "=== Starting Frontend Only ==="
echo.
echo [INFO] Starting frontend development server...
echo [DEBUG] Frontend path: %~dp0frontend

if not exist "%~dp0frontend\package.json" (
    call :log "[ERROR] Frontend package.json not found"
    echo [ERROR] Frontend project not found!
    pause
    goto main
)

start "AI Chat - Frontend" cmd /k "cd /d ""%~dp0frontend"" && npm run dev"
call :log "Frontend process started"
echo [SUCCESS] Frontend service started
echo.
echo Access: http://localhost:5175
echo.
pause
goto main

:start_backend
cls
call :log "=== Starting Backend Only ==="
echo.
echo [INFO] Starting backend service...
echo [DEBUG] Backend path: %~dp0backend\aichat

if not exist "%~dp0backend\aichat\pom.xml" (
    call :log "[ERROR] Backend pom.xml not found"
    echo [ERROR] Backend project not found!
    pause
    goto main
)

start "AI Chat - Backend" cmd /k "cd /d %~dp0backend\aichat && mvn spring-boot:run"
call :log "Backend process started"
echo [SUCCESS] Backend service started
echo.
echo Backend: http://localhost:8080
echo.
pause
goto main

:open_browser
cls
echo [INFO] Opening browser to application...
start http://localhost:5175
call :log "Browser opened"
echo [SUCCESS] Browser opened
echo.
pause
goto main

:open_miniprogram
cls
echo ==========================================
echo        Opening Mini Program Interface
echo ==========================================
echo.
echo [INFO] Checking if frontend service is running...
netstat -ano | findstr ":5175" >nul
if errorlevel 1 (
    echo [WARNING] Frontend service is not running
    echo [INFO] Starting frontend service first...
    start "AI Chat - Frontend" cmd /k "cd /d ""%~dp0frontend"" && npm run dev"
    echo [WAIT] Waiting 8 seconds for frontend to start...
    timeout /t 8 /nobreak >nul
)

echo [INFO] Opening Mini Program interface...
start http://localhost:5175/#/miniprogram
call :log "Mini Program interface opened"
echo [SUCCESS] Mini Program interface opened
echo.
echo Access: http://localhost:5175/#/miniprogram
echo.
pause
goto main

:check_status
cls
echo ==========================================
echo           Service Status Check
echo ==========================================
echo.
call :check_status_detailed
echo.
pause
goto main

:check_status_detailed
call :log "Checking service status..."
echo.
echo Frontend Services (ports 5173-5176):
netstat -ano | findstr ":5173" >nul
if not errorlevel 1 (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173"') do (
        echo [RUNNING] Frontend on port 5173 (PID: %%a)
        call :log "Frontend running on port 5173 (PID: %%a)"
    )
) else (
    echo [STOPPED] Frontend on port 5173
    call :log "Frontend stopped on port 5173"
)

netstat -ano | findstr ":5174" >nul
if not errorlevel 1 (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5174"') do (
        echo [RUNNING] Frontend on port 5174 (PID: %%a)
        call :log "Frontend running on port 5174 (PID: %%a)"
    )
) else (
    echo [STOPPED] Frontend on port 5174
    call :log "Frontend stopped on port 5174"
)

netstat -ano | findstr ":5175" >nul
if not errorlevel 1 (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5175"') do (
        echo [RUNNING] Frontend on port 5175 (PID: %%a)
        call :log "Frontend running on port 5175 (PID: %%a)"
    )
) else (
    echo [STOPPED] Frontend on port 5175
    call :log "Frontend stopped on port 5175"
)

netstat -ano | findstr ":5176" >nul
if not errorlevel 1 (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5176"') do (
        echo [RUNNING] Frontend on port 5176 (PID: %%a)
        call :log "Frontend running on port 5176 (PID: %%a)"
    )
) else (
    echo [STOPPED] Frontend on port 5176
    call :log "Frontend stopped on port 5176"
)

echo.
echo Backend Service (port 8080):
netstat -ano | findstr ":8080" >nul
if not errorlevel 1 (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do (
        echo [RUNNING] Backend on port 8080 (PID: %%a)
        call :log "Backend running on port 8080 (PID: %%a)"
    )
) else (
    echo [STOPPED] Backend on port 8080
    call :log "Backend stopped on port 8080"
)

echo.
echo Summary:
set "frontend_count=0"
set "backend_running=0"
netstat -ano | findstr ":5173" >nul && set "frontend_count=1"
netstat -ano | findstr ":5174" >nul && set "frontend_count=1"
netstat -ano | findstr ":5175" >nul && set "frontend_count=1"
netstat -ano | findstr ":5176" >nul && set "frontend_count=1"
netstat -ano | findstr ":8080" >nul && set "backend_running=1"

if "%frontend_count%"=="1" (
    if "%backend_running%"=="1" (
        echo [ALL RUNNING] Both frontend and backend services are running!
        call :log "All services running"
    ) else (
        echo [PARTIAL] Frontend running but backend stopped
        call :log "Partial: Frontend running, backend stopped"
    )
) else (
    if "%backend_running%"=="1" (
        echo [PARTIAL] Backend running but frontend stopped
        call :log "Partial: Backend running, frontend stopped"
    ) else (
        echo [ALL STOPPED] All services are stopped
        call :log "All services stopped"
    )
)
goto :eof

:stop_all
cls
call :log "=== Stopping All Services ==="
echo ==========================================
echo           Stopping All Services
echo ==========================================
echo.
call :stop_services_detailed
echo.
echo [SUCCESS] All services stopped
call :log "All services stopped"
echo.
pause
goto main

:stop_services_detailed
echo Stopping frontend services...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173"') do (
    echo [KILL] PID %%a (port 5173)
    call :log "Killing PID %%a (port 5173)"
    taskkill /F /PID %%a 2>nul
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5174"') do (
    echo [KILL] PID %%a (port 5174)
    call :log "Killing PID %%a (port 5174)"
    taskkill /F /PID %%a 2>nul
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5175"') do (
    echo [KILL] PID %%a (port 5175)
    call :log "Killing PID %%a (port 5175)"
    taskkill /F /PID %%a 2>nul
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5176"') do (
    echo [KILL] PID %%a (port 5176)
    call :log "Killing PID %%a (port 5176)"
    taskkill /F /PID %%a 2>nul
)

echo Stopping backend service...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do (
    echo [KILL] PID %%a (port 8080)
    call :log "Killing PID %%a (port 8080)"
    taskkill /F /PID %%a 2>nul
)
goto :eof

:stop_services_silent
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5174"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5175"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5176"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do taskkill /F /PID %%a 2>nul
goto :eof

:log
set "TIMESTAMP=%date% %time%"
echo [%TIMESTAMP%] %~1 >> "%LOG_FILE%"
goto :eof

:exit
cls
call :log "=== Exiting Launcher ==="
echo [INFO] Exiting launcher
echo.
echo Log file saved to: %LOG_FILE%
timeout /t 2 /nobreak >nul
exit