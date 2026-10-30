$baseUrl = "http://localhost:8080/api"
$success = 0
$fail = 0
$users = @{}
$timestamp = (Get-Date).Ticks

function Test {
    param(
        [string]$desc,
        [scriptblock]$action
    )
    
    Write-Host ""
    Write-Host "[$desc]" -ForegroundColor Cyan
    
    try {
        & $action
        Write-Host "  PASS" -ForegroundColor Green
        $script:success++
    } catch {
        Write-Host "  FAIL: $_" -ForegroundColor Red
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

$user1name = "user_${timestamp}_1"
$user2name = "user_${timestamp}_2"
$user3name = "user_${timestamp}_3"

Write-Host "TESTING MULTI-USER LOGIN AND OPERATIONS"

Test "User1 Register" {
    $body = @"
{"username":"$user1name","password":"123456","nickname":"User1"}
"@
    $result = Post "$baseUrl/user/register" $body
    $script:users["user1"] = $result.data.id
    Write-Host "  User1 ID: $($result.data.id)"
}

Test "User2 Register" {
    $body = @"
{"username":"$user2name","password":"123456","nickname":"User2"}
"@
    $result = Post "$baseUrl/user/register" $body
    $script:users["user2"] = $result.data.id
    Write-Host "  User2 ID: $($result.data.id)"
}

Test "User3 Register" {
    $body = @"
{"username":"$user3name","password":"123456","nickname":"User3"}
"@
    $result = Post "$baseUrl/user/register" $body
    $script:users["user3"] = $result.data.id
    Write-Host "  User3 ID: $($result.data.id)"
}

Test "User1 Login" {
    $body = @"
{"username":"$user1name","password":"123456"}
"@
    Post "$baseUrl/user/login" $body
}

Test "User2 Login" {
    $body = @"
{"username":"$user2name","password":"123456"}
"@
    Post "$baseUrl/user/login" $body
}

Test "User3 Login" {
    $body = @"
{"username":"$user3name","password":"123456"}
"@
    Post "$baseUrl/user/login" $body
}

Test "User1 Send Request to User2" {
    $body = @"
{"userId":$($users["user1"]),"friendId":$($users["user2"])}
"@
    Post "$baseUrl/friend/request" $body
}

Test "User2 Accept User1" {
    $body = @"
{"userId":$($users["user2"]),"requesterId":$($users["user1"])}
"@
    Post "$baseUrl/friend/accept" $body
}

Test "User1 Chat User2" {
    $body = @"
{"senderId":$($users["user1"]),"receiverId":$($users["user2"]),"content":"Hello!"}
"@
    Post "$baseUrl/friend/chat/send" $body
}

Test "User2 Reply User1" {
    $body = @"
{"senderId":$($users["user2"]),"receiverId":$($users["user1"]),"content":"Hi! Nice to meet you"}
"@
    Post "$baseUrl/friend/chat/send" $body
}

Test "User1 Duplicate Request to User2" {
    try {
        $body = @"
{"userId":$($users["user1"]),"friendId":$($users["user2"])}
"@
        Post "$baseUrl/friend/request" $body
        throw "Should fail"
    } catch {
        Write-Host "  Correctly rejected duplicate"
    }
}

Test "User3 Send Request to User2 (test reject flow)" {
    $body = @"
{"userId":$($users["user3"]),"friendId":$($users["user2"])}
"@
    Post "$baseUrl/friend/request" $body
}

Test "User3 Cannot Chat User2 (not friends yet)" {
    try {
        $body = @"
{"senderId":$($users["user3"]),"receiverId":$($users["user2"]),"content":"Test"}
"@
        Post "$baseUrl/friend/chat/send" $body
        throw "Should fail"
    } catch {
        Write-Host "  Correctly rejected non-friend"
    }
}

Test "User2 Reject User3" {
    $body = @"
{"userId":$($users["user2"]),"requesterId":$($users["user3"])}
"@
    Post "$baseUrl/friend/reject" $body
}

Test "User3 Resend Request After Reject" {
    $body = @"
{"userId":$($users["user3"]),"friendId":$($users["user2"])}
"@
    Post "$baseUrl/friend/request" $body
}

Test "User2 Accept User3" {
    $body = @"
{"userId":$($users["user2"]),"requesterId":$($users["user3"])}
"@
    Post "$baseUrl/friend/accept" $body
}

Test "User3 Chat User2" {
    $body = @"
{"senderId":$($users["user3"]),"receiverId":$($users["user2"]),"content":"Finally friends!"}
"@
    Post "$baseUrl/friend/chat/send" $body
}

Test "User3 Send Request to User1" {
    $body = @"
{"userId":$($users["user3"]),"friendId":$($users["user1"])}
"@
    Post "$baseUrl/friend/request" $body
}

Test "User1 Accept User3" {
    $body = @"
{"userId":$($users["user1"]),"requesterId":$($users["user3"])}
"@
    Post "$baseUrl/friend/accept" $body
}

Test "Get Character List" {
    Get "$baseUrl/character/list"
}

Test "Admin Login" {
    $body = '{"username":"gyb123456","password":"gyb123"}'
    Post "$baseUrl/admin/login" $body
}

Write-Host ""
Write-Host "RESULTS"
Write-Host "Success: $success" -ForegroundColor Green
Write-Host "Fail: $fail" -ForegroundColor Red
$total = $success + $fail
$rate = [math]::Round(($success / $total) * 100, 2)
Write-Host "Success Rate: $rate%" -ForegroundColor Cyan