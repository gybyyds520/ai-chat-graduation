# -*- coding: utf-8 -*-
<#
.SYNOPSIS
  阶段三 P2 本地 CI 流水线脚本（Windows / PowerShell 5.1）。

.DESCRIPTION
  按顺序执行以下步骤，任一步骤失败立即以非 0 退出：
    1. 后端编译   (mvnw -q compile)
    2. 后端测试   (mvnw -q test)                —— 基线 809 用例不回退
    3. 前端测试   (npm run test)                —— 基线 595 用例不回退
    4. 前端构建   (npm run build)               —— 验证路由懒加载分包成功
    5. 自动化检测 (python scripts/run_all_checks.py)
       说明：该步骤依赖已启动的后端（8080 端口）与前端实例；
       若检测到 8080 未监听，则自动跳过并提示，不视为 CI 失败。

.PREREQUISITES
  - JDK 25、Maven Wrapper（backend/aichat/mvnw.cmd）
  - Node.js 18+ / npm（frontend/node_modules 已安装）
  - Python 3（scripts/ 下的检测脚本）

.EXAMPLE
  cd ai-chat-graduation
  powershell -ExecutionPolicy Bypass -File scripts/ci_pipeline.ps1
#>

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot   # 仓库根目录
$backend = Join-Path $root 'backend\aichat'
$frontend = Join-Path $root 'frontend'

function Write-Step($n, $msg) {
    Write-Host ""
    Write-Host ("=" * 60)
    Write-Host (" [{0}] {1}" -f $n, $msg)
    Write-Host ("=" * 60)
}

function Fail($msg) {
    Write-Host "[CI][FAIL] $msg" -ForegroundColor Red
    exit 1
}

# ---------- 1. 后端编译 ----------
Write-Step "1/5" "后端编译 mvnw compile"
Push-Location $backend
try {
    & .\mvnw.cmd -q compile
    if ($LASTEXITCODE -ne 0) { Fail "后端编译失败 (exit=$LASTEXITCODE)" }
} finally { Pop-Location }
Write-Host "[CI] 后端编译 OK" -ForegroundColor Green

# ---------- 2. 后端测试 ----------
Write-Step "2/5" "后端测试 mvnw test"
Push-Location $backend
try {
    & .\mvnw.cmd -q test
    if ($LASTEXITCODE -ne 0) { Fail "后端测试失败 (exit=$LASTEXITCODE)" }
} finally { Pop-Location }
Write-Host "[CI] 后端测试 OK" -ForegroundColor Green

# ---------- 3. 前端测试 ----------
Write-Step "3/5" "前端测试 npm run test"
Push-Location $frontend
try {
    & npm run test
    if ($LASTEXITCODE -ne 0) { Fail "前端测试失败 (exit=$LASTEXITCODE)" }
} finally { Pop-Location }
Write-Host "[CI] 前端测试 OK" -ForegroundColor Green

# ---------- 4. 前端构建（验证懒加载分包） ----------
Write-Step "4/5" "前端构建 npm run build"
Push-Location $frontend
try {
    & npm run build
    if ($LASTEXITCODE -ne 0) { Fail "前端构建失败 (exit=$LASTEXITCODE)" }
} finally { Pop-Location }
Write-Host "[CI] 前端构建 OK" -ForegroundColor Green

# ---------- 5. 自动化健康检测（需运行中的实例） ----------
Write-Step "5/5" "自动化检测 run_all_checks.py"
$port8080 = Test-NetConnection -ComputerName 127.0.0.1 -Port 8080 -WarningAction SilentlyContinue
if (-not $port8080.TcpTestSucceeded) {
    Write-Host "[CI] 未检测到 127.0.0.1:8080 监听，跳过 run_all_checks（需先启动后端）。" -ForegroundColor Yellow
    Write-Host "[CI] 提示：启动后端后可手动执行 python scripts/run_all_checks.py 验证。" -ForegroundColor Yellow
} else {
    & python (Join-Path $root 'scripts\run_all_checks.py')
    if ($LASTEXITCODE -ne 0) { Fail "自动化检测失败 (exit=$LASTEXITCODE)" }
    Write-Host "[CI] 自动化检测 OK" -ForegroundColor Green
}

Write-Host ""
Write-Host "[CI][OK] 全部 CI 步骤通过 ✔" -ForegroundColor Green
exit 0
