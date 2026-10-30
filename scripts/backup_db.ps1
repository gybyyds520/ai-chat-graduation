# ============================================================
# backup_db.ps1 - AI-Chat MySQL 数据库自动备份脚本
#
# 功能：
#   - 使用 mysqldump 备份 ai_chat 数据库到 <项目根>/backups/ 目录
#   - 文件名格式：ai_chat_YYYYMMDD_HHmmss.sql（--full 时为 .sql.gz 压缩包）
#   - 自动清理 7 天以前的旧备份
#   - --full：全量备份 + gzip 压缩
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\backup_db.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\backup_db.ps1 -Full
#
# 环境变量（可选覆盖默认值）：
#   DB_HOST / DB_PORT / DB_USERNAME / DB_PASSWORD / DB_NAME
# ============================================================

param(
    [switch]$Full   # 全量备份 + gzip 压缩
)

$ErrorActionPreference = 'Stop'

# ---------- 配置 ----------
$root       = Split-Path -Parent $PSScriptRoot        # 项目根目录
$backupDir  = Join-Path $root 'backups'
$dbHost     = $env:DB_HOST
if (-not $dbHost)     { $dbHost = 'localhost' }
$dbPort     = $env:DB_PORT
if (-not $dbPort)     { $dbPort = '3306' }
$dbUser     = $env:DB_USERNAME
if (-not $dbUser)     { $dbUser = 'root' }
$dbPass     = $env:DB_PASSWORD
if (-not $dbPass)     { $dbPass = '123456' }          # 与 start-all.ps1 本地默认一致
$dbName     = $env:DB_NAME
if (-not $dbName)     { $dbName = 'ai_chat' }
$retentionDays = 7

# ---------- 输出函数 ----------
function Write-Step($msg) { Write-Host "[backup] $msg" -ForegroundColor Cyan }
function Write-OK($msg)   { Write-Host "[backup] OK  $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "[backup] !!  $msg" -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host "[backup] X  $msg" -ForegroundColor Red }

# ---------- 1. 定位 mysqldump ----------
$mysqldump = $null
$cmd = Get-Command mysqldump -ErrorAction SilentlyContinue
if ($cmd) {
    $mysqldump = $cmd.Source
} else {
    $fallback = @(
        'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe',
        'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqldump.exe',
        'C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysqldump.exe'
    )
    foreach ($f in $fallback) {
        if (Test-Path $f) { $mysqldump = $f; break }
    }
}
if (-not $mysqldump) {
    Write-Err '未找到 mysqldump，请把 MySQL bin 目录加入 PATH 或检查默认安装路径。'
    exit 1
}
Write-Step "mysqldump: $mysqldump"

# ---------- 2. 准备备份目录与文件名 ----------
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$stamp    = Get-Date -Format 'yyyyMMdd_HHmmss'
$sqlFile  = Join-Path $backupDir ("{0}_{1}.sql" -f $dbName, $stamp)
$finalFile = $sqlFile

# ---------- 3. 执行 mysqldump ----------
Write-Step ("开始备份数据库 {0}@{1}:{2}  (Full={3})" -f $dbName, $dbHost, $dbPort, $Full.IsPresent)

# 用 --result-file 直接写文件，避免控制台编码问题
$dumpArgs = @(
    "--host=$dbHost",
    "--port=$dbPort",
    "--user=$dbUser",
    "--password=$dbPass",
    '--single-transaction',   # InnoDB 一致性快照，不锁表
    '--routines',             # 备份存储过程/函数
    '--triggers',
    '--events',
    '--set-gtid-purged=OFF',
    '--default-character-set=utf8mb4',
    '--result-file', $sqlFile,
    $dbName
)

try {
    & $mysqldump @dumpArgs
    if ($LASTEXITCODE -ne 0) {
        throw "mysqldump 退出码 $LASTEXITCODE"
    }
} catch {
    Write-Err "备份执行失败：$($_.Exception.Message)"
    if (Test-Path $sqlFile) { Remove-Item $sqlFile -Force -ErrorAction SilentlyContinue }
    exit 2
}

if (-not (Test-Path $sqlFile) -or ((Get-Item $sqlFile).Length -eq 0)) {
    Write-Err "备份文件未生成或为空：$sqlFile"
    exit 3
}

$sqlSizeMB = [math]::Round((Get-Item $sqlFile).Length / 1MB, 2)
Write-OK ("SQL 备份完成：{0}  ({1} MB)" -f $sqlFile, $sqlSizeMB)

# ---------- 4. --full：gzip 压缩 ----------
if ($Full) {
    $gzFile = "$sqlFile.gz"
    try {
        Write-Step "正在压缩为 $gzFile ..."
        $in  = [System.IO.File]::OpenRead($sqlFile)
        $out = [System.IO.File]::Create($gzFile)
        $gz  = New-Object System.IO.Compression.GzipStream($out, [System.IO.Compression.CompressionLevel]::Optimal)
        $in.CopyTo($gz)
        $gz.Close(); $out.Close(); $in.Close()
        Remove-Item $sqlFile -Force
        $finalFile = $gzFile
        $gzSizeMB = [math]::Round((Get-Item $gzFile).Length / 1MB, 2)
        Write-OK ("压缩完成：{0}  ({1} MB)" -f $gzFile, $gzSizeMB)
    } catch {
        Write-Warn "压缩失败，保留未压缩的 .sql 文件：$($_.Exception.Message)"
        if (Test-Path $gzFile) { Remove-Item $gzFile -Force -ErrorAction SilentlyContinue }
    }
}

# ---------- 5. 清理 N 天前的旧备份 ----------
Write-Step "清理 $retentionDays 天前的旧备份 ..."
$cutoff = (Get-Date).AddDays(-$retentionDays)
$oldFiles = Get-ChildItem -Path $backupDir -Filter "$dbName*.sql*" -File |
            Where-Object { $_.LastWriteTime -lt $cutoff }
foreach ($old in $oldFiles) {
    Remove-Item $old.FullName -Force
    Write-OK "已删除旧备份：$($old.Name)"
}
if (-not $oldFiles) { Write-Step "无过期备份需要清理。" }

# ---------- 6. 汇总 ----------
$finalSize = [math]::Round((Get-Item $finalFile).Length / 1MB, 2)
Write-Host ""
Write-Host "================ 备份完成 ================" -ForegroundColor Green
Write-OK "备份文件: $finalFile"
Write-OK "文件大小: $finalSize MB"
Write-OK "备份目录: $backupDir"
Write-Host "=========================================="
exit 0
