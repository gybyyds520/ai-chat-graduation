@echo off
chcp 65001 >nul
title AI Chat 移动端访问授权
echo ============================================
echo  AI Chat 移动端访问授权（防火墙放行）
echo  作用：允许手机通过局域网访问本项目
echo  端口：5187（网页）/ 8080（接口）
echo ============================================
echo.

:: 检查是否管理员权限
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo 需要管理员权限，正在请求授权窗口...
    echo 请在弹窗中点击"是"
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

echo 正在添加防火墙规则...
netsh advfirewall firewall add rule name="AI Chat Web 5187" dir=in action=allow protocol=TCP localport=5187
netsh advfirewall firewall add rule name="AI Chat API 8080" dir=in action=allow protocol=TCP localport=8080

echo.
echo ============================================
echo  已完成！手机请连接与电脑相同的 Wi-Fi，
echo  浏览器访问：http://172.16.188.50:5187
echo ============================================
echo.
pause
