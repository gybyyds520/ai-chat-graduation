@echo off
chcp 65001 >nul
title AI Chat 应用启动器

:main
cls
echo.
echo ==========================================
echo        AI Chat 桌面应用启动器
echo ==========================================
echo.
echo   [1] 启动完整应用（前端 + 后端）
echo   [2] 仅启动前端开发服务器
echo   [3] 仅启动后端服务
echo   [4] 打开浏览器访问应用
echo   [5] 查看服务状态
echo   [6] 停止所有服务
echo   [0] 退出
echo.
echo ==========================================
echo.

set /p choice="请选择操作 (0-6): "

if "%choice%"=="1" goto start_all
if "%choice%"=="2" goto start_frontend
if "%choice%"=="3" goto start_backend
if "%choice%"=="4" goto open_browser
if "%choice%"=="5" goto check_status
if "%choice%"=="6" goto stop_all
if "%choice%"=="0" goto exit
goto main

:start_all
cls
echo [INFO] 正在启动完整应用...
echo.
echo [1/2] 启动后端服务...
start "AI Chat - 后端" cmd /k "cd /d %~dp0backend\aichat && mvn spring-boot:run"
echo [等待] 等待后端服务启动...
timeout /t 10 /nobreak >nul

echo [2/2] 启动前端开发服务器...
start "AI Chat - 前端" cmd /k "cd /d %~dp0frontend && npm run dev"
echo [等待] 等待前端服务启动...
timeout /t 5 /nobreak >nul

echo [SUCCESS] 应用启动成功！
echo.
echo 访问地址: http://localhost:5175
echo 后端API: http://localhost:8080
echo.
pause
goto main

:start_frontend
cls
echo [INFO] 正在启动前端开发服务器...
start "AI Chat - 前端" cmd /k "cd /d %~dp0frontend && npm run dev"
echo [SUCCESS] 前端服务已启动
echo.
echo 访问地址: http://localhost:5175
echo.
pause
goto main

:start_backend
cls
echo [INFO] 正在启动后端服务...
start "AI Chat - 后端" cmd /k "cd /d %~dp0backend\aichat && mvn spring-boot:run"
echo [SUCCESS] 后端服务已启动
echo.
echo 后端API: http://localhost:8080
echo.
pause
goto main

:open_browser
cls
echo [INFO] 正在打开浏览器...
start http://localhost:5175
echo [SUCCESS] 浏览器已打开
echo.
pause
goto main

:check_status
cls
echo [INFO] 检查服务状态...
echo.
echo 检查前端服务 (端口 5173-5176)...
netstat -ano | findstr ":5173" >nul && echo [RUNNING] 前端服务 (5173) || echo [STOPPED] 前端服务 (5173)
netstat -ano | findstr ":5174" >nul && echo [RUNNING] 前端服务 (5174) || echo [STOPPED] 前端服务 (5174)
netstat -ano | findstr ":5175" >nul && echo [RUNNING] 前端服务 (5175) || echo [STOPPED] 前端服务 (5175)
netstat -ano | findstr ":5176" >nul && echo [RUNNING] 前端服务 (5176) || echo [STOPPED] 前端服务 (5176)
echo.
echo 检查后端服务 (端口 8080)...
netstat -ano | findstr ":8080" >nul && echo [RUNNING] 后端服务 (8080) || echo [STOPPED] 后端服务 (8080)
echo.
pause
goto main

:stop_all
cls
echo [INFO] 正在停止所有服务...
echo.
echo 查找并停止占用端口的进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5174"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5175"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5176"') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do taskkill /F /PID %%a 2>nul

echo [SUCCESS] 所有服务已停止
echo.
pause
goto main

:exit
cls
echo [INFO] 退出启动器
echo.
timeout /t 2 /nobreak >nul
exit