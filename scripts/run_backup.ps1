# ============================================================
# run_backup.ps1 - 一键备份入口
#
# 调用 backup_db.ps1 执行 MySQL 备份，输出结果；失败时打印错误信息并以非零码退出。
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\run_backup.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\run_backup.ps1 -Full
# ============================================================

param(
    [switch]$Full   # 透传给 backup_db.ps1：全量备份 + gzip 压缩
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$backupScript = Join-Path $PSScriptRoot 'backup_db.ps1'

function Write-Title($msg) { Write-Host "[run_backup] $msg" -ForegroundColor Cyan }
function Write-OK($msg)     { Write-Host "[run_backup] OK  $msg" -ForegroundColor Green }
function Write-Err($msg)    { Write-Host "[run_backup] X  $msg" -ForegroundColor Red }

if (-not (Test-Path $backupScript)) {
    Write-Err "未找到备份脚本：$backupScript"
    exit 1
}

Write-Title "开始执行数据库备份 ..."
$argList = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $backupScript)
if ($Full) { $argList += '-Full' }

# 调用 backup_db.ps1；其 stdout/stderr 直接透传到本控制台
$proc = Start-Process -FilePath 'powershell.exe' -ArgumentList $argList -NoNewWindow -Wait -PassThru

if ($proc.ExitCode -eq 0) {
    Write-Host ""
    Write-OK "备份任务执行成功。"
    # 列出 backups 目录最新文件，便于确认产物
    $latest = Get-ChildItem (Join-Path $root 'backups') -Filter 'ai_chat*.sql*' -File -ErrorAction SilentlyContinue |
              Sort-Object LastWriteTime -Descending | Select-Object -First 3
    if ($latest) {
        Write-OK "最近备份文件："
        foreach ($f in $latest) {
            $sizeMB = [math]::Round($f.Length / 1MB, 2)
            Write-Host ("    - {0}  ({1} MB, {2})" -f $f.Name, $sizeMB, $f.LastWriteTime) -ForegroundColor Gray
        }
    }
    exit 0
} else {
    Write-Host ""
    Write-Err "备份任务失败，退出码 = $($proc.ExitCode)"
    Write-Err "请检查：MySQL 服务是否启动、DB_PASSWORD 是否正确、mysqldump 是否可用、backups 目录是否可写。"
    exit $proc.ExitCode
}
