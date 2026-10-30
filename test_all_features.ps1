$baseUrl = "http://localhost:8080/api"
$success = 0
$fail = 0

function Test-Request {
    param(
        [string]$name,
        [string]$method,
        [string]$url,
        [string]$body
    )
    
    Write-Host ""
    Write-Host "Test: " $name
    
    try {
        if ($method -eq "POST") {
            $response = Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json"
        } else {
            $response = Invoke-RestMethod -Uri $url -Method Get
        }
        
        if ($response.code -eq 200) {
            Write-Host "OK"
            $script:success++
        } else {
            Write-Host "FAIL: " $response.message
            $script:fail++
        }
    } catch {
        Write-Host "ERROR: $_"
        $script:fail++
    }
}

Write-Host "Starting comprehensive tests..."

Test-Request -name "User Register testuser3" -method "POST" -url "$baseUrl/user/register" -body '{"username":"testuser3","password":"123456","nickname":"Test3"}'
Test-Request -name "User Login testuser3" -method "POST" -url "$baseUrl/user/login" -body '{"username":"testuser3","password":"123456"}'
Test-Request -name "Get Character List" -method "GET" -url "$baseUrl/character/list" -body ""
Test-Request -name "Admin Login" -method "POST" -url "$baseUrl/admin/login" -body '{"username":"gyb123456","password":"gyb123"}'

Write-Host ""
Write-Host "Results: Success=$success, Fail=$fail"
$rate = [math]::Round(($success / ($success + $fail)) * 100, 2)
Write-Host "Success Rate: $rate%"