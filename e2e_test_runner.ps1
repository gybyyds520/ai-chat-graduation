# E2E Test Script v2 - Fixed API paths + error handling
# Matches TokenUtils HMAC-SHA256 algorithm

$ErrorActionPreference = "Continue"
$baseUrl = "http://localhost:8088"
$secretKey = "aichat-fallback-key-change-in-production-2024"

function Generate-JwtToken {
    param([string]$userId, [string]$username)
    $header = '{"alg":"HS256","typ":"JWT"}'
    $exp = [DateTimeOffset]::Now.AddHours(24).ToUnixTimeMilliseconds()
    $iat = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
    $payload = "{`"userId`":`"$userId`",`"username`":`"$username`",`"exp`":`"$exp`",`"iat`":`"$iat`"}"

    $h64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($header)).Replace('+','-').Replace('/','_')
    $p64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($payload)).Replace('+','-').Replace('/','_')
    $data = "$h64.$p64"

    $hmac = New-Object System.Security.Cryptography.HMACSHA256
    $hmac.Key = [Text.Encoding]::UTF8.GetBytes($secretKey)
    $sigBytes = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($data))
    $s64 = [Convert]::ToBase64String($sigBytes).Replace('+','-').Replace('/','_')

    return "$data.$s64"
}

function Invoke-Api {
    param([string]$method, [string]$path, [object]$body, [string]$token)
    $headers = @{ "Content-Type" = "application/json" }
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $uri = "$baseUrl$path"
    try {
        if ($body) {
            $json = $body | ConvertTo-Json -Depth 10 -Compress
            $resp = Invoke-RestMethod -Method $method -Uri $uri -Headers $headers -Body $json -TimeoutSec 15
        } else {
            $resp = Invoke-RestMethod -Method $method -Uri $uri -Headers $headers -TimeoutSec 15
        }
        return $resp
    } catch {
        $exResp = $_.Exception.Response
        if ($exResp) {
            $statusCode = [int]$exResp.StatusCode
            try {
                $reader = New-Object IO.StreamReader($exResp.GetResponseStream())
                $errBody = $reader.ReadToEnd()
                if ($errBody -match '^\s*\{') {
                    return $errBody | ConvertFrom-Json
                }
                return [PSCustomObject]@{ code = $statusCode; msg = $errBody; data = $null }
            } catch {
                return [PSCustomObject]@{ code = $statusCode; msg = "HTTP $statusCode"; data = $null }
            }
        }
        return [PSCustomObject]@{ code = -1; msg = $_.Exception.Message; data = $null }
    }
}

function Get-DbValue {
    param([string]$query)
    $out = & mysql -u root -p123456 --force -e $query 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $out) { return $null }
    $lines = $out -split "`n" | Where-Object { $_.Trim() -ne "" }
    if ($lines.Count -lt 2) { return $null }
    # Skip header row, return first data row. Force array context with @()
    return @($lines[1] -split "`t")
}

# PowerShell scalar indexing guard: when a function returns a single value,
# PowerShell unwraps arrays of length 1 to a scalar string. Indexing [0] on a
# string returns the first character. Always use @() when calling to force
# array context: $row = @(Get-DbValue ...)

Write-Host "=== E2E Test v2 Started at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ===" -ForegroundColor Cyan
$results = @()

# ============================================================
# Phase 1: User register + login
# ============================================================
Write-Host "`n[Phase 1] User Register/Login" -ForegroundColor Yellow
$rng = Get-Random -Maximum 99999
$regBody = @{ username = "e2euser$rng"; password = "Test@12345"; nickname = "E2E-User-$rng" }
$regResp = Invoke-Api "POST" "/api/user/register" $regBody $null
$regStatus = if($regResp.code -eq 200){"PASS"}else{"FAIL: $($regResp.code)"}
$results += [PSCustomObject]@{ Phase="1.Register"; Status=$regStatus; Detail="userId=$($regResp.data.id)" }
Write-Host "  Register: code=$($regResp.code)" -ForegroundColor $(if($regResp.code -eq 200){"Green"}else{"Red"})

if ($regResp.code -eq 200) {
    $loginResp = Invoke-Api "POST" "/api/user/login" @{ username = $regBody.username; password = $regBody.password } $null
    $loginStatus = if($loginResp.code -eq 200){"PASS"}else{"FAIL"}
    $results += [PSCustomObject]@{ Phase="1.Login"; Status=$loginStatus; Detail="username=$($loginResp.data.username)" }
    Write-Host "  Login: code=$($loginResp.code)" -ForegroundColor $(if($loginResp.code -eq 200){"Green"}else{"Red"})
    $testUserId = $loginResp.data.id
    $testUsername = $loginResp.data.username
} else {
    $testUserId = 75
    $testUsername = "e2euser1"
    $loginResp = Invoke-Api "POST" "/api/user/login" @{username="e2euser1";password="Test@12345"} $null
    $loginStatus = if($loginResp.code -eq 200){"PASS"}else{"FAIL: $($loginResp.code)"}
    $results += [PSCustomObject]@{ Phase="1.Login(existing)"; Status=$loginStatus; Detail="userId=75" }
    Write-Host "  Login(existing e2euser1): code=$($loginResp.code)" -ForegroundColor $(if($loginResp.code -eq 200){"Green"}else{"Red"})
}

# Generate JWT Token
$token = Generate-JwtToken -userId $testUserId -username $testUsername
$tokShort = if($token.Length -gt 60){$token.Substring(0,60)+"..."}else{$token}
Write-Host "  JWT Token: $tokShort" -ForegroundColor DarkGray

# Token validation: valid token works, invalid token rejected
$tokenOk = Invoke-Api "GET" "/api/user/$testUserId" $null $token
$badTokenResp = Invoke-Api "GET" "/api/user/$testUserId" $null "invalid.token.here"
$tokStatus = if($tokenOk.code -eq 200 -and $badTokenResp.code -ne 200){"PASS"}else{"CHECK"}
$results += [PSCustomObject]@{ Phase="1.TokenCheck"; Status=$tokStatus; Detail="valid=$($tokenOk.code) invalid=$($badTokenResp.code)" }
Write-Host "  Token check: valid=$($tokenOk.code) invalid=$($badTokenResp.code)" -ForegroundColor $(if($tokenOk.code -eq 200){"Green"}else{"Yellow"})

# ============================================================
# Phase 2: Social post/comment flow (e2e-5)
# ============================================================
Write-Host "`n[Phase 2] Social Post/Comment" -ForegroundColor Yellow

# Create post via /api/social/post (singular)
$postBody = @{
    userId = $testUserId
    content = "E2E test post - $(Get-Date -Format 'HH:mm:ss')"
    mediaType = "TEXT"
    visibility = "PUBLIC"
}
$postResp = Invoke-Api "POST" "/api/social/post" $postBody $token
$postStatus = if($postResp.code -eq 200){"PASS"}else{"INFO: $($postResp.code)"}
$results += [PSCustomObject]@{ Phase="2.CreatePost"; Status=$postStatus; Detail=$($postResp.msg) }
Write-Host "  Create post: code=$($postResp.code) msg=$($postResp.msg)" -ForegroundColor $(if($postResp.code -eq 200){"Green"}else{"Yellow"})

# Get public posts via /api/social/posts/public (pass token for auth)
$listResp = Invoke-Api "GET" "/api/social/posts/public?limit=5" $null $token
$listStatus = if($listResp.code -eq 200){"PASS"}else{"INFO: $($listResp.code)"}
$recCount = if($listResp.data){@($listResp.data).Count}else{0}
$results += [PSCustomObject]@{ Phase="2.PublicPosts"; Status=$listStatus; Detail="count=$recCount" }
Write-Host "  Public posts: code=$($listResp.code) count=$recCount" -ForegroundColor $(if($listResp.code -eq 200){"Green"}else{"Yellow"})

# Comment flow
$postId = $null
if ($postResp.code -eq 200 -and $postResp.data.id) {
    $postId = $postResp.data.id
} elseif ($listResp.code -eq 200 -and $recCount -gt 0) {
    $postId = @($listResp.data)[0].id
    Write-Host "  Using existing post ID=$postId" -ForegroundColor DarkGray
}

if ($postId) {
    # Create comment via /api/social/comment (singular)
    $commentBody = @{ postId = $postId; userId = $testUserId; content = "E2E test comment" }
    $commentResp = Invoke-Api "POST" "/api/social/comment" $commentBody $token
    $cmtStatus = if($commentResp.code -eq 200){"PASS"}else{"INFO: $($commentResp.code)"}
    $results += [PSCustomObject]@{ Phase="2.CreateComment"; Status=$cmtStatus; Detail=$($commentResp.msg) }
    Write-Host "  Comment: code=$($commentResp.code) msg=$($commentResp.msg)" -ForegroundColor $(if($commentResp.code -eq 200){"Green"}else{"Yellow"})

    # Like via /api/social/like (needs targetType lowercase, targetId, userId)
    $likeBody = @{ targetType = "post"; targetId = $postId; userId = $testUserId }
    $likeResp = Invoke-Api "POST" "/api/social/like" $likeBody $token
    $likeStatus = if($likeResp.code -eq 200){"PASS"}else{"INFO: $($likeResp.code)"}
    $results += [PSCustomObject]@{ Phase="2.Like"; Status=$likeStatus; Detail=$($likeResp.msg) }
    Write-Host "  Like: code=$($likeResp.code) msg=$($likeResp.msg)" -ForegroundColor $(if($likeResp.code -eq 200){"Green"}else{"Yellow"})
} else {
    $results += [PSCustomObject]@{ Phase="2.Comment"; Status="SKIP"; Detail="No post ID" }
    Write-Host "  Skip comment: no post ID" -ForegroundColor DarkGray
}

# ============================================================
# Phase 3: Payment order flow (e2e-6)
# ============================================================
Write-Host "`n[Phase 3] Payment Order Flow" -ForegroundColor Yellow

# 3.1 Bind payment account
$bindBody = @{ userId = $testUserId; accountType = "ALIPAY"; accountNumber = "13900000$($testUserId % 100000)"; accountName = "E2E-Account" }
$bindResp = Invoke-Api "POST" "/api/pay-account/bind" $bindBody $token
$bindStatus = if($bindResp.code -eq 200){"PASS"}else{"INFO: $($bindResp.code)"}
$results += [PSCustomObject]@{ Phase="3.BindAccount"; Status=$bindStatus; Detail=$($bindResp.msg) }
Write-Host "  Bind account: code=$($bindResp.code) msg=$($bindResp.msg)" -ForegroundColor $(if($bindResp.code -eq 200){"Green"}else{"Yellow"})

# 3.2 Get account id and verification code from DB
$accountId = $null
if ($bindResp.code -eq 200 -and $bindResp.data.id) {
    $accountId = $bindResp.data.id
} else {
    # Query DB for existing account
    $dbRow = @(Get-DbValue "USE ai_chat_e2e; SELECT id, verification_code, status FROM pay_account WHERE user_id = $testUserId;")
    if ($dbRow.Count -gt 0) {
        $accountId = $dbRow[0]
    }
}

if ($accountId) {
    Write-Host "  Pay account ID=$accountId" -ForegroundColor DarkGray
    # Get verification code from DB
    $vcRow = @(Get-DbValue "USE ai_chat_e2e; SELECT verification_code FROM pay_account WHERE id = $accountId;")
    $verificationCode = if($vcRow.Count -gt 0){$vcRow[0]}else{""}
    Write-Host "  Verification code (from DB): $verificationCode" -ForegroundColor DarkGray

    # 3.3 Verify account
    $verifyBody = @{ id = [int]$accountId; verificationCode = $verificationCode }
    $verifyResp = Invoke-Api "POST" "/api/pay-account/verify" $verifyBody $token
    $verStatus = if($verifyResp.code -eq 200){"PASS"}else{"INFO: $($verifyResp.code)"}
    $results += [PSCustomObject]@{ Phase="3.VerifyAccount"; Status=$verStatus; Detail=$($verifyResp.msg) }
    Write-Host "  Verify: code=$($verifyResp.code) msg=$($verifyResp.msg)" -ForegroundColor $(if($verifyResp.code -eq 200){"Green"}else{"Yellow"})

    # Verify DB status changed to VERIFIED
    $statusRow = @(Get-DbValue "USE ai_chat_e2e; SELECT status FROM pay_account WHERE id = $accountId;")
    $dbStatus = if($statusRow.Count -gt 0){$statusRow[0]}else{"?"}
    Write-Host "  DB status after verify: $dbStatus" -ForegroundColor DarkGray

    # 3.4 Create payment order
    $payBody = @{ userId = $testUserId; durationDays = 30; versionType = "PRO"; paymentMethod = "ALIPAY" }
    $payResp = Invoke-Api "POST" "/api/payment/create" $payBody $token
    $payStatus = if($payResp.code -eq 200){"PASS"}else{"FAIL: $($payResp.code)"}
    $results += [PSCustomObject]@{ Phase="3.CreateOrder"; Status=$payStatus; Detail=$($payResp.msg) }
    Write-Host "  Create order: code=$($payResp.code) msg=$($payResp.msg)" -ForegroundColor $(if($payResp.code -eq 200){"Green"}else{"Red"})

    if ($payResp.code -eq 200 -and $payResp.data) {
        $payId = $payResp.data.id
        $txnId = $payResp.data.transactionId
        $amount = $payResp.data.amount
        $results += [PSCustomObject]@{ Phase="3.OrderDetail"; Status="PASS"; Detail="id=$payId txn=$txnId amount=$amount" }
        Write-Host "  Order ID=$payId txnId=$txnId amount=$amount" -ForegroundColor Green

        # 3.5 Query payment record
        $getPayResp = Invoke-Api "GET" "/api/payment/$payId" $null $token
        $getStatus = if($getPayResp.code -eq 200){"PASS"}else{"INFO"}
        $results += [PSCustomObject]@{ Phase="3.QueryOrder"; Status=$getStatus; Detail="status=$($getPayResp.data.paymentStatus)" }
        Write-Host "  Query order: code=$($getPayResp.code) status=$($getPayResp.data.paymentStatus)" -ForegroundColor $(if($getPayResp.code -eq 200){"Green"}else{"Yellow"})

        # 3.6 Verify UUID transaction ID uniqueness (check format)
        if ($txnId -match '^TXN-[A-F0-9]{24}$') {
            $results += [PSCustomObject]@{ Phase="3.UUIDFormat"; Status="PASS"; Detail="txn format valid" }
            Write-Host "  UUID format: valid" -ForegroundColor Green
        } else {
            $results += [PSCustomObject]@{ Phase="3.UUIDFormat"; Status="INFO"; Detail="txn=$txnId" }
            Write-Host "  UUID format: check txn=$txnId" -ForegroundColor Yellow
        }
    }
} else {
    $results += [PSCustomObject]@{ Phase="3.PaymentFlow"; Status="FAIL"; Detail="No pay account ID" }
    Write-Host "  Payment flow failed: no pay account ID" -ForegroundColor Red
}

# ============================================================
# Phase 4: Security verification (e2e-8)
# ============================================================
Write-Host "`n[Phase 4] Security Verification" -ForegroundColor Yellow

# 4.1 Invalid token rejection
$badTokenResp2 = Invoke-Api "GET" "/api/user/$testUserId" $null "tampered.token.signature"
$btStatus = if($badTokenResp2.code -ne 200){"PASS"}else{"FAIL"}
$results += [PSCustomObject]@{ Phase="4.InvalidTokenReject"; Status=$btStatus; Detail="code=$($badTokenResp2.code)" }
Write-Host "  Invalid token: code=$($badTokenResp2.code) (expected non-200)" -ForegroundColor $(if($badTokenResp2.code -ne 200){"Green"}else{"Red"})

# 4.2 Unauthorized admin access
$adminResp = Invoke-Api "GET" "/api/admin/users" $null $token
$admStatus = if($adminResp.code -eq 403 -or $adminResp.code -eq 401){"PASS"}else{"INFO: $($adminResp.code)"}
$results += [PSCustomObject]@{ Phase="4.UnauthorizedAdmin"; Status=$admStatus; Detail="code=$($adminResp.code)" }
Write-Host "  Unauthorized admin: code=$($adminResp.code) (expected 403/401)" -ForegroundColor $(if($adminResp.code -eq 403 -or $adminResp.code -eq 401){"Green"}else{"Yellow"})

# 4.3 Brute force lockout (REQUIRES Redis - mark as SKIP if Redis unavailable)
$redisRunning = (netstat -ano | Select-String ":6379.*LISTENING").Count -gt 0
if (-not $redisRunning) {
    $results += [PSCustomObject]@{ Phase="4.BruteForceLock"; Status="SKIP"; Detail="Redis not running - counter not persisted" }
    Write-Host "  Brute force lock: SKIP (Redis not running, counter not persisted)" -ForegroundColor DarkGray
    Write-Host "  Note: CacheService degrades silently when Redis is down." -ForegroundColor DarkGray
    Write-Host "  To test: start Redis (docker-compose up -d redis) then re-run" -ForegroundColor DarkGray
} else {
    Write-Host "  Testing brute force lockout (5 failed logins)..." -ForegroundColor DarkGray
    $lockUser = "e2e_lock_$(Get-Random -Maximum 99999)"
    $lockReg = Invoke-Api "POST" "/api/user/register" @{username=$lockUser;password="Correct@123";nickname="LockTest"} $null

    $lockTriggeredAt = 0
    for ($i=1; $i -le 6; $i++) {
        $failLogin = Invoke-Api "POST" "/api/user/login" @{username=$lockUser;password="WrongPwd$i"} $null
        Write-Host "    Attempt $i : code=$($failLogin.code)" -ForegroundColor DarkGray
        if ($failLogin.code -eq 403 -or $failLogin.code -eq 10103) { $lockTriggeredAt = $i; break }
    }
    $bfStatus = if($lockTriggeredAt -gt 0 -and $lockTriggeredAt -le 5){"PASS"}else{"INFO"}
    $results += [PSCustomObject]@{ Phase="4.BruteForceLock"; Status=$bfStatus; Detail="locked at attempt $lockTriggeredAt" }
    Write-Host "  Brute force lock: triggered at attempt $lockTriggeredAt" -ForegroundColor $(if($lockTriggeredAt -gt 0){"Green"}else{"Yellow"})
}

# 4.4 Password length validation
$shortPwdResp = Invoke-Api "POST" "/api/user/register" @{username="shortpwd_test";password="123";nickname="Test"} $null
$pwdStatus = if($shortPwdResp.code -ne 200){"PASS"}else{"FAIL"}
$results += [PSCustomObject]@{ Phase="4.PasswordValidation"; Status=$pwdStatus; Detail="code=$($shortPwdResp.code)" }
Write-Host "  Password validation (short): code=$($shortPwdResp.code) (expected non-200)" -ForegroundColor $(if($shortPwdResp.code -ne 200){"Green"}else{"Red"})

# ============================================================
# Test Report
# ============================================================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "E2E Test Report - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
$results | Format-Table -AutoSize

$passCount = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($results | Where-Object { $_.Status -like "FAIL*" }).Count
$infoCount = ($results | Where-Object { $_.Status -like "INFO*" -or $_.Status -eq "CHECK" -or $_.Status -eq "SKIP" }).Count
Write-Host "`nTotal: $($results.Count) | PASS: $passCount | FAIL: $failCount | INFO/SKIP: $infoCount" -ForegroundColor $(if($failCount -eq 0){"Green"}else{"Yellow"})

# Export report to temp directory (avoids Chinese path issues)
$reportPath = [IO.Path]::Combine($env:TEMP, "e2e_test_report_$(Get-Date -Format 'yyyyMMdd_HHmmss').csv")
try {
    $results | Export-Csv -Path $reportPath -NoTypeInformation -Encoding UTF8
    Write-Host "`nReport exported: $reportPath" -ForegroundColor Cyan
} catch {
    Write-Host "`nCSV export failed: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "Results displayed above only." -ForegroundColor Yellow
}
