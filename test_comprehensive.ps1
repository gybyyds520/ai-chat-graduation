$baseUrl = "http://localhost:8080/api"
$success = 0
$fail = 0
$testRounds = 3

function Test {
    param(
        [string]$desc,
        [scriptblock]$action
    )
    
    try {
        & $action
        Write-Host "PASS: $desc" -ForegroundColor Green
        $script:success++
    } catch {
        Write-Host "FAIL: $desc - $_" -ForegroundColor Red
        $script:fail++
    }
}

function Post {
    param(
        [string]$url,
        [string]$body
    )
    
    $response = Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json"
    if ($response.code -ne 200) {
        throw $response.message
    }
    return $response
}

function Get {
    param(
        [string]$url
    )
    
    $response = Invoke-RestMethod -Uri $url -Method Get
    if ($response.code -ne 200) {
        throw $response.message
    }
    return $response
}

Write-Host "=== COMPREHENSIVE TEST - Multiple User Login Scenarios ==="

for ($round = 1; $round -le $testRounds; $round++) {
    Write-Host ""
    Write-Host "=== Round $round ===" -ForegroundColor Cyan
    
    $users = @{}
    $timestamp = (Get-Date).Ticks
    
    Test "Register User1" {
        $body = '{"username":"user_' + $timestamp + '_1","password":"123456","nickname":"User1"}'
        $result = Post "$baseUrl/user/register" $body
        $users["user1"] = $result.data.id
    }

    Test "Register User2" {
        $body = '{"username":"user_' + $timestamp + '_2","password":"123456","nickname":"User2"}'
        $result = Post "$baseUrl/user/register" $body
        $users["user2"] = $result.data.id
    }

    Test "Register User3" {
        $body = '{"username":"user_' + $timestamp + '_3","password":"123456","nickname":"User3"}'
        $result = Post "$baseUrl/user/register" $body
        $users["user3"] = $result.data.id
    }

    Test "User1 Login" {
        $body = '{"username":"user_' + $timestamp + '_1","password":"123456"}'
        Post "$baseUrl/user/login" $body
    }

    Test "User2 Login" {
        $body = '{"username":"user_' + $timestamp + '_2","password":"123456"}'
        Post "$baseUrl/user/login" $body
    }

    Test "User3 Login" {
        $body = '{"username":"user_' + $timestamp + '_3","password":"123456"}'
        Post "$baseUrl/user/login" $body
    }

    Test "User1 Send Request to User2" {
        $body = '{"userId":' + $users["user1"] + ',"friendId":' + $users["user2"] + '}'
        Post "$baseUrl/friend/request" $body
    }

    Test "User2 Accept User1" {
        $body = '{"userId":' + $users["user2"] + ',"requesterId":' + $users["user1"] + '}'
        Post "$baseUrl/friend/accept" $body
    }

    Test "User1 Chat User2" {
        $body = '{"senderId":' + $users["user1"] + ',"receiverId":' + $users["user2"] + ',"content":"Hello"}'
        Post "$baseUrl/friend/chat/send" $body
    }

    Test "User3 Send Request to User2" {
        $body = '{"userId":' + $users["user3"] + ',"friendId":' + $users["user2"] + '}'
        Post "$baseUrl/friend/request" $body
    }

    Test "User2 Reject User3" {
        $body = '{"userId":' + $users["user2"] + ',"requesterId":' + $users["user3"] + '}'
        Post "$baseUrl/friend/reject" $body
    }

    Test "User3 Resend After Reject" {
        $body = '{"userId":' + $users["user3"] + ',"friendId":' + $users["user2"] + '}'
        Post "$baseUrl/friend/request" $body
    }

    Test "User2 Accept User3" {
        $body = '{"userId":' + $users["user2"] + ',"requesterId":' + $users["user3"] + '}'
        Post "$baseUrl/friend/accept" $body
    }

    Test "Duplicate Request Rejected" {
        try {
            $body = '{"userId":' + $users["user1"] + ',"friendId":' + $users["user2"] + '}'
            Post "$baseUrl/friend/request" $body
            throw "Should fail"
        } catch {
            Write-Host "  Correctly rejected"
        }
    }

    Start-Sleep -Milliseconds 300
}

Write-Host ""
Write-Host "=== Edge Case Tests ===" -ForegroundColor Cyan

Test "Get Character List" {
    Get "$baseUrl/character/list"
}

Test "Admin Login" {
    $body = '{"username":"gyb123456","password":"gyb123"}'
    Post "$baseUrl/admin/login" $body
}

Write-Host ""
Write-Host "=== Invalid Input Tests ===" -ForegroundColor Cyan

Test "Empty Username Login" {
    try {
        $body = '{"username":"","password":"123456"}'
        Post "$baseUrl/user/login" $body
        throw "Should fail"
    } catch {
        Write-Host "  Correctly rejected"
    }
}

Test "Non-existent User Login" {
    try {
        $body = '{"username":"nonexistent","password":"123456"}'
        Post "$baseUrl/user/login" $body
        throw "Should fail"
    } catch {
        Write-Host "  Correctly rejected"
    }
}

Write-Host ""
Write-Host "=== TEST RESULTS ==="
Write-Host "Total: " ($success + $fail)
Write-Host "Success: $success" -ForegroundColor Green
Write-Host "Fail: $fail" -ForegroundColor Red
$rate = [math]::Round(($success / ($success + $fail)) * 100, 2)
Write-Host "Success Rate: $rate%" -ForegroundColor Cyan