$baseUrl = "http://localhost:8080/api"

function Cleanup-Friendship {
    param(
        [int]$userId,
        [int]$friendId
    )
    
    try {
        $body = @{
            userId = $userId
            friendId = $friendId
        } | ConvertTo-Json
        
        Invoke-RestMethod -Uri "$baseUrl/friend/remove" -Method Post -Body $body -ContentType "application/json" | Out-Null
    } catch {
        # Ignore cleanup errors
    }
}

function Test-FriendRequestPending {
    param(
        [int]$round
    )
    
    Write-Host ""
    Write-Host "============================================================"
    Write-Host "Round " $round " : User not accepting friend request scenario"
    Write-Host "============================================================"

    $userAId = 1
    $userBId = 2

    Cleanup-Friendship -userId $userAId -friendId $userBId
    Cleanup-Friendship -userId $userBId -friendId $userAId

    Write-Host ""
    Write-Host "[Step 1] User " $userAId " sends friend request to user " $userBId
    $requestBody = @{
        userId = $userAId
        friendId = $userBId
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/friend/request" -Method Post -Body $requestBody -ContentType "application/json"
        Write-Host "Result: " $response.message
        
        if ($response.code -eq 200) {
            Write-Host "OK - Friend request sent successfully"
        } else {
            Write-Host "FAIL - Friend request failed: " $response.message
            return $false
        }
    } catch {
        Write-Host "FAIL - Request error: $_"
        return $false
    }

    Write-Host ""
    Write-Host "[Step 2] User " $userAId " sends request again (test duplicate handling)"
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/friend/request" -Method Post -Body $requestBody -ContentType "application/json"
        Write-Host "Result: " $response.message
        
        if ($response.code -ne 200) {
            Write-Host "OK - Duplicate request rejected correctly"
        } else {
            Write-Host "FAIL - Duplicate request not rejected"
            return $false
        }
    } catch {
        Write-Host "FAIL - Request error: $_"
        return $false
    }

    Write-Host ""
    Write-Host "[Step 3] User " $userAId " tries to send message (test non-friend chat)"
    $messageBody = @{
        senderId = $userAId
        receiverId = $userBId
        content = "Hello, are you there?"
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/friend/chat/send" -Method Post -Body $messageBody -ContentType "application/json"
        Write-Host "Result: " $response.message
        
        if ($response.code -ne 200) {
            Write-Host "OK - Non-friend message rejected correctly"
        } else {
            Write-Host "FAIL - Non-friend message sent successfully"
            return $false
        }
    } catch {
        Write-Host "OK - Non-friend message rejected (exception caught)"
    }

    Write-Host ""
    Write-Host "[Step 4] Check pending requests for user " $userBId
    try {
        $uri = "$baseUrl/friend/requests?userId=$userBId"
        $response = Invoke-RestMethod -Uri $uri -Method Get
        $pendingCount = $response.data.Count
        Write-Host "Pending requests count: " $pendingCount
        
        if ($pendingCount -gt 0) {
            Write-Host "OK - Pending requests list contains request"
        } else {
            Write-Host "FAIL - Pending requests list is empty"
            return $false
        }
    } catch {
        Write-Host "FAIL - Request error: $_"
        return $false
    }

    Write-Host ""
    Write-Host "[Step 5] Check friend status"
    try {
        $uri = "$baseUrl/friend/status?userId=$userAId&targetId=$userBId"
        $response = Invoke-RestMethod -Uri $uri -Method Get
        $status = $response.data
        Write-Host "User " $userAId " view status with user " $userBId ": " $status
        
        if ($status -eq "pending_sent") {
            Write-Host "OK - Status is correct: pending_sent"
        } else {
            Write-Host "FAIL - Status incorrect: " $status
            return $false
        }
    } catch {
        Write-Host "FAIL - Request error: $_"
        return $false
    }

    try {
        $uri = "$baseUrl/friend/status?userId=$userBId&targetId=$userAId"
        $response = Invoke-RestMethod -Uri $uri -Method Get
        $status = $response.data
        Write-Host "User " $userBId " view status with user " $userAId ": " $status
        
        if ($status -eq "pending_received") {
            Write-Host "OK - Status is correct: pending_received"
        } else {
            Write-Host "FAIL - Status incorrect: " $status
            return $false
        }
    } catch {
        Write-Host "FAIL - Request error: $_"
        return $false
    }

    Write-Host ""
    Write-Host "[Step 6] User " $userBId " rejects friend request"
    $rejectBody = @{
        userId = $userBId
        requesterId = $userAId
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/friend/reject" -Method Post -Body $rejectBody -ContentType "application/json"
        Write-Host "Result: " $response.message
        
        if ($response.code -eq 200) {
            Write-Host "OK - Friend request rejected successfully"
        } else {
            Write-Host "FAIL - Friend request reject failed"
            return $false
        }
    } catch {
        Write-Host "FAIL - Request error: $_"
        return $false
    }

    Write-Host ""
    Write-Host "[Step 7] User " $userAId " sends request again after rejection"
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/friend/request" -Method Post -Body $requestBody -ContentType "application/json"
        Write-Host "Result: " $response.message
        
        if ($response.code -eq 200) {
            Write-Host "OK - Resend request after rejection successful"
        } else {
            Write-Host "FAIL - Resend request after rejection failed"
            return $false
        }
    } catch {
        Write-Host "FAIL - Request error: $_"
        return $false
    }

    Write-Host ""
    Write-Host "============================================================"
    Write-Host "Round " $round " completed"
    Write-Host "============================================================"
    return $true
}

Write-Host "Start testing user not accepting friend request scenarios"
Write-Host "Target: Reproduce the 6 failed test scenarios"
Write-Host "Test rounds: 10"

$successCount = 0
$failCount = 0
$totalRounds = 10

for ($i = 1; $i -le $totalRounds; $i++) {
    if (Test-FriendRequestPending -round $i) {
        $successCount++
    } else {
        $failCount++
    }
    
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Write-Host "============================================================"
Write-Host "Test Results Summary"
Write-Host "============================================================"
Write-Host "Total rounds: " $totalRounds
Write-Host "Success: " $successCount
Write-Host "Fail: " $failCount
$successRate = [math]::Round(($successCount / $totalRounds) * 100, 2)
Write-Host "Success rate: " $successRate "%"
Write-Host "============================================================"