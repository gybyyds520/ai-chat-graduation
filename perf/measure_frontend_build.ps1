# 前端 build 基线测量：vite build 时长 + dist 产物体积
$ErrorActionPreference = 'Continue'
Set-Location 'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\frontend'

# 记录 build 前 dist 状态
$before = Get-ChildItem -Path '.\dist' -Recurse -File -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum
Write-Host ('dist before: files=' + $before.Count + ' bytes=' + $before.Sum)

$sw = [System.Diagnostics.Stopwatch]::StartNew()
npm run build 2>&1 | Out-File -FilePath '..\perf\raw_vite_build.log' -Encoding utf8
$code = $LASTEXITCODE
$sw.Stop()

Write-Host ('build_exit=' + $code + ' wall_ms=' + [math]::Round($sw.Elapsed.TotalMilliseconds,1))

# dist 产物体积
$after = Get-ChildItem -Path '.\dist' -Recurse -File -ErrorAction SilentlyContinue
$sum = ($after | Measure-Object -Property Length -Sum).Sum
Write-Host ('dist after: files=' + $after.Count + ' bytes=' + $sum + ' MB=' + [math]::Round($sum/1MB,2))

# 各 JS/CSS 资产 top 10
$after | Where-Object { $_.Extension -in '.js','.css','.html' } |
    Sort-Object Length -Descending | Select-Object -First 10 |
    ForEach-Object { Write-Host ('asset: ' + $_.FullName.Replace((Get-Location).Path + '\dist\','') + ' = ' + [math]::Round($_.Length/1KB,1) + ' KB') }
