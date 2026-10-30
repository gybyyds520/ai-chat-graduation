# AI Chat 一键启动器（核心逻辑）
# 由 启动AI-Chat.bat 调用。幂等：组件已在运行则跳过启动。

param(
    [switch]$NoBrowser,   # 测试用：不自动打开浏览器
    [switch]$Monitor      # 可选：同时启动 Prometheus 监控（默认不启，仅毕设演示无需监控）
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot          # 项目根目录
$backend = Join-Path $root 'backend\aichat'
$frontend = Join-Path $root 'frontend'
$logsDir = Join-Path $root 'logs'
New-Item -ItemType Directory -Force -Path $logsDir | Out-Null

$DB_PASSWORD = '123456'   # 本地 MySQL root 密码（与后端 application.yml 一致）

function Write-Step($msg) { Write-Host "[AI Chat] $msg" -ForegroundColor Cyan }
function Write-OK($msg)   { Write-Host "[AI Chat] OK  $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "[AI Chat] !!  $msg" -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host "[AI Chat] X  $msg" -ForegroundColor Red }

function Test-Port($port) {
    return [bool](Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
}

function Wait-Port($port, $timeoutSec, $label) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $port) { return $true }
        Start-Sleep -Milliseconds 1500
    }
    return $false
}

function Show-LogTail($logFile, $lines) {
    if (Test-Path $logFile) {
        Write-Host "    日志尾部（$logFile）:" -ForegroundColor DarkYellow
        Get-Content $logFile -Tail $lines | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkYellow }
    }
}

Write-Host ""
Write-Host "============== AI Chat 一键启动 ==============" -ForegroundColor Cyan
Write-Host "  项目目录: $root"
Write-Host "  启动组件: MySQL / Redis / Ollama / 后端(8080) / 前端(5187) / 桌面端"
Write-Host "=============================================="

# ---------- 1. MySQL ----------
$mysqlSvc = Get-Service -Name 'MySQL80' -ErrorAction SilentlyContinue
if (-not $mysqlSvc) { $mysqlSvc = Get-Service | Where-Object { $_.Name -match '^mysql' } | Select-Object -First 1 }
if ($mysqlSvc) {
    if ($mysqlSvc.Status -eq 'Running') {
        Write-OK "MySQL 服务运行中 ($($mysqlSvc.Name))"
    } else {
        Write-Step "正在启动 MySQL 服务 ($($mysqlSvc.Name)) ..."
        try {
            Start-Service -Name $mysqlSvc.Name -ErrorAction Stop
            Write-OK "MySQL 服务已启动"
        } catch {
            Write-Err "MySQL 服务启动失败，请以管理员身份手动启动：net start $($mysqlSvc.Name)"
        }
    }
    # 刚开机场景：服务状态 Running 但端口可能尚未监听 → 等待 3306 就绪
    if (-not (Test-Port 3306)) {
        Write-Step "等待 MySQL 就绪 (3306) ..."
        if (Wait-Port 3306 60 'MySQL') { Write-OK "MySQL 端口就绪 (3306)" }
        else { Write-Warn "MySQL 端口 60 秒内未就绪，后端可能启动失败，请稍后重试" }
    }
} else {
    Write-Err "未找到 MySQL 服务，请确认已安装 MySQL"
}

# ---------- 2. Redis（缓存依赖，后端 cache.type=redis 需要） ----------
if (Test-Port 6379) {
    Write-OK "Redis 运行中 (6379)"
} else {
    $redisExe = Join-Path $root 'ops\redis\redis-server.exe'
    if (Test-Path $redisExe) {
        Write-Step "正在启动 Redis (6379) ..."
        Start-Process -FilePath $redisExe -ArgumentList 'redis-project.conf' -WorkingDirectory (Join-Path $root 'ops\redis') -WindowStyle Hidden
        if (Wait-Port 6379 30 'Redis') { Write-OK "Redis 就绪 (6379)" }
        else { Write-Warn "Redis 30 秒内未就绪，后端缓存可能不可用（不影响核心聊天功能）" }
    } else {
        Write-Warn "未找到 Redis（ops\redis\redis-server.exe），后端缓存降级；如需完整体验请保留 ops\redis 目录"
    }
}

# ---------- 3. Ollama（可选，AI 对话引擎） ----------
if (Test-Port 11434) {
    Write-OK "Ollama 运行中 (11434)"
} else {
    $ollamaCmd = Get-Command ollama -ErrorAction SilentlyContinue
    if ($ollamaCmd) {
        Write-Step "正在启动 Ollama ..."
        Start-Process -FilePath $ollamaCmd.Source -ArgumentList 'serve' -WindowStyle Hidden
        if (Wait-Port 11434 25 'Ollama') {
            Write-OK "Ollama 已就绪 (11434)"
        } else {
            Write-Warn "Ollama 25 秒内未就绪，如稍后聊天无响应请手动运行: ollama serve"
        }
        # 检查对话模型是否已拉取（llama3.2:3b 等），未拉取时给出明确指引
        try {
            $ollamaList = @(& $ollamaCmd.Source list 2>$null)
            $hasModel = ($ollamaList | Select-String -Pattern 'llama3|phi3|qwen') -ne $null
            if (-not $hasModel) {
                Write-Warn "未检测到对话模型。首次使用请运行: ollama pull llama3.2:3b"
                Write-Warn "拉取完成后再启动本程序即可获得完整 AI 对话能力"
            }
        } catch { }
    } else {
        Write-Warn "未找到 Ollama：聊天将使用降级模式；如需完整 AI 对话请安装 Ollama"
    }
}

# ---------- 4. 后端 ----------
$backendLog = Join-Path $logsDir 'backend.log'
if (Test-Port 8080) {
    Write-OK "后端已运行 (8080)"
} else {
    Write-Step "正在启动后端 (Spring Boot, 8080)，首次启动约需 30~90 秒 ..."
    # 用 PowerShell 环境变量继承方式传递 DB_PASSWORD（避免 cmd 的
    # "set DB_PASSWORD=123456&& mvnw ..." 写法在误加空格时把尾随空格带入
    # 密码，导致 Access denied 的经典坑；实测出现过）
    $env:DB_PASSWORD = $DB_PASSWORD
    Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', "mvnw.cmd spring-boot:run > `"$backendLog`" 2>&1" -WorkingDirectory $backend -WindowStyle Hidden
    if (Wait-Port 8080 150 '后端') {
        Write-OK "后端就绪 (8080)"
        # 健康自检：Redis 指示器已在 application.yml 关闭（兼容 Redis 5 INFO 解析缺陷），
        # 此时 /actuator/health 应为 {"status":"UP"}（用 curl 取字符串输出，
        # 避免 PowerShell 5.1 Invoke-WebRequest .Content 返回字节数组导致 -match 误判）
        Start-Sleep -Seconds 3
        try {
            $health = & curl.exe -s -m 8 'http://127.0.0.1:8080/actuator/health' 2>$null
            if ($health -match '"status":"UP"') { Write-OK "后端健康检查 UP" }
            else { Write-Warn "后端健康检查非 UP: $health（应用可能仍在初始化，稍后可用）" }
        } catch {
            Write-Warn "后端健康检查暂不可用（应用可能仍在初始化）"
        }
    } else {
        Write-Err "后端启动失败或超时，请查看日志：$backendLog"
        Show-LogTail $backendLog 15
    }
}

# ---------- 5. 前端 ----------
$frontendLog = Join-Path $logsDir 'frontend.log'
if (Test-Port 5187) {
    Write-OK "前端已运行 (5187)"
} else {
    Write-Step "正在启动前端 (Vite, 5187) ..."
    Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', "npm run dev -- --host 0.0.0.0 > `"$frontendLog`" 2>&1" -WorkingDirectory $frontend -WindowStyle Hidden
    if (Wait-Port 5187 120 '前端') {
        Write-OK "前端就绪 (5187)"
    } else {
        Write-Err "前端启动失败或超时，请查看日志：$frontendLog"
        Show-LogTail $frontendLog 15
    }
}

# ---------- 6. Electron 桌面端 ----------
$electronLog = Join-Path $logsDir 'electron.log'
# 已运行检测：按项目路径匹配（避免最小化到托盘/无标题时误判，也避免误判其他 Electron 应用）
$electronProc = Get-CimInstance Win32_Process -Filter "Name='electron.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match 'ai-chat-graduation' -or $_.ExecutablePath -match 'ai-chat-graduation' -or $_.CommandLine -match 'frontend[\\/]electron' }
if ($electronProc) {
    Write-OK "桌面端已在运行 (Electron, PID $($electronProc[0].ProcessId))"
} else {
    Write-Step "正在启动桌面端 (Electron) ..."
    # 直接调用 electron.exe 并注入 NODE_ENV（绕开 cmd/electron.cmd 批处理层，避免中文路径编码损坏）
    # 注意：不能用 -WindowStyle Hidden——Electron 是 GUI 应用，Hidden 会隐藏主窗口导致"黑屏/无窗口"
    $electronExe = Join-Path $frontend 'node_modules\electron\dist\electron.exe'
    if (Test-Path $electronExe) {
        $env:NODE_ENV = 'development'
        Start-Process -FilePath $electronExe -ArgumentList '.' -WorkingDirectory $frontend
    } else {
        Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', "npm run electron:dev > `"$electronLog`" 2>&1" -WorkingDirectory $frontend -WindowStyle Hidden
    }
    # 轮询等待主窗口出现（最多 40 秒，区分"启动慢"与"启动失败"）
    $deadline = (Get-Date).AddSeconds(40)
    $winFound = $false
    while ((Get-Date) -lt $deadline) {
        $w = Get-Process electron -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -ne '' }
        if ($w) { $winFound = $true; break }
        Start-Sleep -Milliseconds 1500
    }
    if ($winFound) {
        Write-OK "桌面端已启动 (Electron, 窗口: $($w[0].MainWindowTitle))"
    } else {
        Write-Warn "桌面端未检测到窗口，日志：$electronLog"
        Show-LogTail $electronLog 8
    }
}

# ---------- 7. 可选：Prometheus 监控（-Monitor 参数） ----------
$promOk = $false
if ($Monitor) {
    $promDir = Join-Path $root 'ops\prometheus\prometheus-2.54.1.windows-amd64'
    $promExe = Join-Path $promDir 'prometheus.exe'
    if (Test-Port 9090) {
        Write-OK "Prometheus 运行中 (9090)"
        $promOk = $true
    } elseif (Test-Path $promExe) {
        Write-Step "正在启动 Prometheus 监控 (9090) ..."
        Start-Process -FilePath $promExe -ArgumentList '--config.file=ai-chat.yml' -WorkingDirectory $promDir -WindowStyle Hidden
        if (Wait-Port 9090 30 'Prometheus') { Write-OK "Prometheus 就绪 (9090)，面板 http://localhost:9090"; $promOk = $true }
        else { Write-Warn "Prometheus 未就绪，请查看 ops\prometheus 目录日志" }
    } else {
        Write-Warn "未找到 Prometheus（ops\prometheus），跳过监控启动"
    }
} else {
    Write-Step "提示: 需要监控时请运行「启动AI-Chat.bat monitor」或本脚本加 -Monitor 参数"
}

# ---------- 8. 汇总 ----------
$webOk = Test-Port 5187
$apiOk = Test-Port 8080
$ollamaOk = Test-Port 11434
$redisOk = Test-Port 6379
# 桌面端确认：按项目路径匹配进程 + 主窗口已创建（有非空窗口标题）才算启动成功
$desktopProc = Get-CimInstance Win32_Process -Filter "Name='electron.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match 'ai-chat-graduation' -or $_.ExecutablePath -match 'ai-chat-graduation' -or $_.CommandLine -match 'frontend[\\/]electron' }
$desktopWin = Get-Process electron -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -ne '' }
$desktopOk = [bool]($desktopProc -and $desktopWin)
# 后端健康检查（Redis 指示器已在配置关闭，正常应为 UP；用 curl 取字符串输出）
$apiHealthOk = $false
if ($apiOk) {
    try {
        $healthBody = & curl.exe -s -m 8 'http://127.0.0.1:8080/actuator/health' 2>$null
        $apiHealthOk = ($healthBody -match '"status":"UP"')
    } catch { }
}
# Redis 连通性（PING 验证，端口监听不代表可用）
$redisPingOk = $false
if ($redisOk) {
    $redisCli = Join-Path $root 'ops\redis\redis-cli.exe'
    if (Test-Path $redisCli) {
        $redisPingOk = (& $redisCli -h 127.0.0.1 -p 6379 ping 2>$null) -match 'PONG'
    }
}
# Ollama 对话模型检查（llama3.2:3b 等）
$ollamaModels = @()
try { $ollamaModels = @(ollama list 2>$null | Select-String -Pattern 'llama3|phi3|qwen' | ForEach-Object { ($_ -split '\s+')[0] }) } catch { }

Write-Host ""
Write-Host "================ 三端启动完成 ================" -ForegroundColor Green
if ($apiOk)  { Write-OK "后端 API    http://localhost:8080" } else { Write-Err "后端 API    未就绪" }
if ($apiHealthOk) { Write-OK "后端健康    /actuator/health = UP" } elseif ($apiOk) { Write-Warn "后端健康    非 UP（应用可能仍在初始化）" }
if ($webOk)  { Write-OK "网页端      http://localhost:5187" } else { Write-Err "网页端      未就绪" }
if ($redisPingOk) { Write-OK "缓存        Redis 6379 PONG" } elseif ($redisOk) { Write-Warn "缓存        Redis 6379 监听但 PING 异常" } else { Write-Warn "缓存        Redis 未运行（后端缓存降级）" }
if ($ollamaOk) { Write-OK "AI 引擎     Ollama 11434（对话模型: $($ollamaModels -join ', ')）" } else { Write-Warn "AI 引擎     Ollama 未运行（降级模式）" }
if ($desktopOk) { Write-OK "桌面端      Electron 已启动（窗口: $($desktopWin[0].MainWindowTitle)）" } else { Write-Warn "桌面端      Electron 未启动（可手动运行 npm run electron:dev）" }
if ($promOk)  { Write-OK "监控        Prometheus http://localhost:9090" }
Write-Host ""
Write-Host "  手机访问: http://172.16.188.50:5187 （手机连同一 Wi-Fi）"
Write-Host "  测试账号: demo_alice / Test@123456"
Write-Host "  日志目录: $logsDir"
Write-Host "  停止项目: 双击「停止AI-Chat.bat」"
Write-Host "========================================"

if ($webOk -and -not $NoBrowser) {
    Write-Step "正在打开浏览器 ..."
    Start-Process 'http://localhost:5187'
}

if (-not $webOk -or -not $apiOk) {
    Write-Err "部分组件未就绪，请查看上方日志定位问题；也可再次双击启动器（已运行组件会自动跳过）"
}
