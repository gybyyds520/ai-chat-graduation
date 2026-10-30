# 性能基线测量脚本（只读，不修改生产代码）
# 目标：POST /api/user/login 延迟原始样本
# 注意：authApi 限流器 = 10 次/分钟（全局），脚本总请求数控制 <= 8
param(
    [string]$Base = 'http://localhost:8080',
    [int]$Warmup = 1,
    [int]$Samples = 5,
    [int]$SpacingMs = 2000,
    [int]$Failpath = 1,
    [string]$OutFile = ''
)

$results = @()
function Invoke-TimedLogin {
    param([string]$User, [string]$Pass)
    $body = (@{ username = $User; password = $Pass } | ConvertTo-Json -Compress)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $status = 0; $code = $null; $err = $null
    try {
        $resp = Invoke-WebRequest -Uri ($Base + '/api/user/login') -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 15 -UseBasicParsing
        $sw.Stop()
        $status = [int]$resp.StatusCode
        try { $j = $resp.Content | ConvertFrom-Json; $code = $j.code } catch {}
    } catch {
        $sw.Stop()
        if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
        $err = $_.Exception.Message
    }
    $o = New-Object psobject
    $o | Add-Member NoteProperty ms ([math]::Round($sw.Elapsed.TotalMilliseconds,2))
    $o | Add-Member NoteProperty status $status
    $o | Add-Member NoteProperty code $code
    $o | Add-Member NoteProperty err $err
    $o | Add-Member NoteProperty user $User
    return $o
}

for ($i = 1; $i -le $Warmup; $i++) {
    $r = Invoke-TimedLogin -User 'admin' -Pass '123456'
    $rr = New-Object psobject
    $rr | Add-Member NoteProperty phase 'warmup'
    $rr | Add-Member NoteProperty idx $i
    $rr | Add-Member NoteProperty ms $r.ms
    $rr | Add-Member NoteProperty status $r.status
    $rr | Add-Member NoteProperty code $r.code
    $rr | Add-Member NoteProperty err $r.err
    $rr | Add-Member NoteProperty user $r.user
    $results += $rr
    Write-Host ('warmup ' + $i + ' : ' + ($rr | ConvertTo-Json -Compress))
    Start-Sleep -Milliseconds $SpacingMs
}

$users = @(
    @('demo_alice','Test@123456'),
    @('demo_bob','Test@123456'),
    @('demo_carol','Test@123456'),
    @('admin','123456'),
    @('demo_dave','Test@123456')
)
for ($i = 0; $i -lt $Samples; $i++) {
    $u = $users[$i % $users.Count]
    $r = Invoke-TimedLogin -User $u[0] -Pass $u[1]
    $rr = New-Object psobject
    $rr | Add-Member NoteProperty phase 'sample'
    $rr | Add-Member NoteProperty idx ($i+1)
    $rr | Add-Member NoteProperty ms $r.ms
    $rr | Add-Member NoteProperty status $r.status
    $rr | Add-Member NoteProperty code $r.code
    $rr | Add-Member NoteProperty err $r.err
    $rr | Add-Member NoteProperty user $r.user
    $results += $rr
    Write-Host ('sample ' + ($i+1) + ' : ' + ($rr | ConvertTo-Json -Compress))
    Start-Sleep -Milliseconds $SpacingMs
}

for ($i = 1; $i -le $Failpath; $i++) {
    $r = Invoke-TimedLogin -User 'demo_frank' -Pass ('WrongPass_' + $i)
    $rr = New-Object psobject
    $rr | Add-Member NoteProperty phase 'failpath'
    $rr | Add-Member NoteProperty idx $i
    $rr | Add-Member NoteProperty ms $r.ms
    $rr | Add-Member NoteProperty status $r.status
    $rr | Add-Member NoteProperty code $r.code
    $rr | Add-Member NoteProperty err $r.err
    $rr | Add-Member NoteProperty user $r.user
    $results += $rr
    Write-Host ('failpath ' + $i + ' : ' + ($rr | ConvertTo-Json -Compress))
    Start-Sleep -Milliseconds $SpacingMs
}

if ($OutFile -ne '') {
    $results | ConvertTo-Json -Depth 4 | Out-File -FilePath $OutFile -Encoding utf8
}

Write-Host '--- SUMMARY ---'
$ok = @($results | Where-Object { $_.phase -eq 'sample' -and $_.status -eq 200 })
if ($ok.Count -gt 0) {
    $vals = @($ok | ForEach-Object { $_.ms } | Sort-Object)
    $n = $vals.Count
    $mid = [int][math]::Floor(($n - 1) / 2)
    Write-Host ('n=' + $n + ' min=' + $vals[0] + ' median=' + $vals[$mid] + ' max=' + $vals[$n-1])
}
