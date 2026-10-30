# AI Chat 一键停止器（核心逻辑）
# 由 停止AI-Chat.bat 调用。停止顺序：桌面端 Electron → 后端(8080) → 前端(5187) → 残留清理。
# 用法：stop-all.ps1 [-All]    # -All 表示连 MySQL / Ollama 系统服务一起停止

param([switch]$All)

function Write-OK($m)   { Write-Host "[停止] OK  $m" -ForegroundColor Green }
function Write-Warn($m) { Write-Host "[停止] !!  $m" -ForegroundColor Yellow }
function Write-Err($m)  { Write-Host "[停止] X  $m" -ForegroundColor Red }
function Write-Step($m) { Write-Host "[停止] $m" -ForegroundColor Cyan }
function Test-Port($p)  { return [bool](Get-NetTCPConnection -State Listen -LocalPort $p -ErrorAction SilentlyContinue) }

Write-Host ""
Write-Host "============== AI Chat 一键停止 ==============" -ForegroundColor Cyan

# ---------- 1. 停止桌面端 Electron（本项目相关） ----------
# 匹配依据：CommandLine 或 ExecutablePath 含项目路径（双保险，防未来启动方式
# 变化导致 CommandLine 不含路径时漏杀；同时避免误杀 VS Code 等无关 Electron 应用）
$elecs = Get-CimInstance Win32_Process -Filter "Name='electron.exe'" -ErrorAction SilentlyContinue |
    Where-Object { ($_.CommandLine -match 'ai-chat-graduation') -or ($_.ExecutablePath -match 'ai-chat-graduation') -or ($_.CommandLine -match 'frontend[\\/]electron') }
if ($elecs) {
    foreach ($e in $elecs) {
        try { Stop-Process -Id $e.ProcessId -Force -ErrorAction Stop; Write-OK "桌面端 Electron 已停止 (PID $($e.ProcessId))" }
        catch { Write-Warn "桌面端停止失败 (PID $($e.ProcessId)): $($_.Exception.Message)" }
    }
} else {
    Write-OK "桌面端 Electron 未在运行"
}

# ---------- 2. 停止 Redis(6379)（仅本项目启动的 redis-server） ----------
$redisProcs = Get-CimInstance Win32_Process -Filter "Name='redis-server.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match 'ai-chat-graduation' -or $_.CommandLine -match 'redis-project.conf' }
if ($redisProcs) {
    foreach ($r in $redisProcs) {
        try { Stop-Process -Id $r.ProcessId -Force -ErrorAction Stop; Write-OK "Redis 已停止 (PID $($r.ProcessId))" }
        catch { Write-Warn "Redis 停止失败 (PID $($r.ProcessId)): $($_.Exception.Message)" }
    }
} else {
    $rconn = Get-NetTCPConnection -State Listen -LocalPort 6379 -ErrorAction SilentlyContinue
    if ($rconn) { Write-Warn "6379 被其他程序占用，未自动停止（可在任务管理器手动结束）" }
    else { Write-OK "Redis 未在运行" }
}

# ---------- 3. 停止后端(8080) 与 前端(5187) ----------
foreach ($p in @(8080, 5187)) {
    $conns = Get-NetTCPConnection -State Listen -LocalPort $p -ErrorAction SilentlyContinue
    if (-not $conns) { Write-OK "端口 $p 未在监听（已停止）"; continue }
    foreach ($c in $conns) {
        try {
            Stop-Process -Id $c.OwningProcess -Force -ErrorAction Stop
            Write-OK "端口 $p 已停止 (PID $($c.OwningProcess))"
        } catch {
            Write-Err "端口 $p 停止失败: $($_.Exception.Message)"
        }
    }
}

# ---------- 4. 清理残留 Node 进程（本项目 vite/npm 相关） ----------
$nodes = Get-CimInstance Win32_Process -Filter "Name='node.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match 'ai-chat-graduation' -or $_.CommandLine -match 'vite' }
foreach ($n in $nodes) {
    try { Stop-Process -Id $n.ProcessId -Force -ErrorAction Stop; Write-OK "残留 Node 进程已清理 (PID $($n.ProcessId))" }
    catch { }
}
Start-Sleep -Milliseconds 800

# ---------- 5. 可选：停止系统服务（MySQL / Ollama / 监控） ----------
if ($All) {
    Write-Step "按 -All 参数：同时停止 MySQL、Ollama、Prometheus、Grafana ..."
    $svc = Get-Service -Name 'MySQL80' -ErrorAction SilentlyContinue
    if ($svc -and $svc.Status -eq 'Running') {
        try { Stop-Service -Name $svc.Name -Force -ErrorAction Stop; Write-OK "MySQL 服务已停止 ($($svc.Name))" }
        catch { Write-Warn "MySQL 停止失败（可能需要管理员权限）: $($_.Exception.Message)" }
    } else {
        Write-OK "MySQL 服务未在运行"
    }
    $ollama = Get-Process ollama -ErrorAction SilentlyContinue
    if ($ollama) {
        $ollama | Stop-Process -Force -ErrorAction SilentlyContinue
        Write-OK "Ollama 已停止"
    } else {
        Write-OK "Ollama 未在运行"
    }
    # 监控组件（仅停止本项目路径下的 Prometheus / Grafana）
    $promProcs = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object { ($_.Name -match '^prometheus' -or $_.Name -match 'grafana') -and $_.CommandLine -match 'ai-chat-graduation' }
    if ($promProcs) {
        foreach ($p in $promProcs) {
            try { Stop-Process -Id $p.ProcessId -Force -ErrorAction Stop; Write-OK "监控组件已停止 ($($p.Name) PID $($p.ProcessId))" }
            catch { Write-Warn "监控组件停止失败 ($($p.Name)): $($_.Exception.Message)" }
        }
    } else {
        Write-OK "Prometheus / Grafana 未在运行"
    }
} else {
    Write-OK "MySQL / Ollama / 监控为系统级组件，本次未停止（如需一并停止：停止AI-Chat.bat all）"
}

# ---------- 6. 汇总 ----------
$left = @(8080, 5187, 6379) | Where-Object { Test-Port $_ }
if ($left) {
    Write-Warn "以下端口仍在监听: $($left -join ', ')（可能被其他程序占用，可手动在任务管理器结束）"
} else {
    Write-OK "项目已全部停止，端口 8080 / 5187 / 6379 已释放"
}
Write-Host "============================================" -ForegroundColor Cyan
