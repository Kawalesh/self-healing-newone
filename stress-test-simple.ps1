# ================================
# Simple Stress Test - Quick Run
# Usage:
#   PowerShell: .\stress-test-simple.ps1
#   CMD: powershell -ExecutionPolicy Bypass -File stress-test-simple.ps1
# ================================

$UserServiceUrl  = "http://localhost:8081"
$OrderServiceUrl = "http://localhost:8082"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "QUICK STRESS TEST" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# -------------------------------
# Check User Service
# -------------------------------
Write-Host "Checking services..." -ForegroundColor Yellow

try {
    Invoke-WebRequest "$UserServiceUrl/actuator/health" -TimeoutSec 5 | Out-Null
    Write-Host "✓ User Service is available" -ForegroundColor Green
} catch {
    Write-Host "✗ User Service is not available" -ForegroundColor Red
    Write-Host "Run: docker-compose up -d" -ForegroundColor Yellow
    exit 1
}

# -------------------------------
# Check Order Service
# -------------------------------
try {
    Invoke-WebRequest "$OrderServiceUrl/actuator/health" -TimeoutSec 5 | Out-Null
    Write-Host "✓ Order Service is available" -ForegroundColor Green
} catch {
    Write-Host "✗ Order Service is not available" -ForegroundColor Red
    Write-Host "Run: docker-compose up -d" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Testing User Service..." -ForegroundColor Yellow
Write-Host "Creating 10 users..." -ForegroundColor Gray

$userCount  = 0
$userErrors = 0

# -------------------------------
# Create Users
# -------------------------------
foreach ($num in 1..10) {
    $body = @{
        username  = "user$num"
        email     = "user$num@test.com"
        firstName = "User"
        lastName  = "$num"
    } | ConvertTo-Json

    try {
        $result = Invoke-RestMethod `
            -Uri "$UserServiceUrl/api/users" `
            -Method POST `
            -Body $body `
            -ContentType "application/json"

        Write-Host "  ✓ Created user $num (ID: $($result.id))" -ForegroundColor Green
        $userCount++
    } catch {
        Write-Host "  ✗ Failed to create user $num" -ForegroundColor Red
        $userErrors++
    }
}

Write-Host ""
Write-Host "User Service Results: $userCount created, $userErrors failed" `
    -ForegroundColor ($(if ($userErrors -eq 0) { "Green" } else { "Yellow" }))

# -------------------------------
# Fetch Users
# -------------------------------
Write-Host ""
Write-Host "Testing Order Service..." -ForegroundColor Yellow

try {
    $users = Invoke-RestMethod "$UserServiceUrl/api/users"
    $userId = $users[0].id
    Write-Host "Using User ID: $userId" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed to fetch users" -ForegroundColor Red
    exit 1
}

# -------------------------------
# Create Orders
# -------------------------------
Write-Host "Creating 20 orders..." -ForegroundColor Gray

$orderCount  = 0
$orderErrors = 0

foreach ($num in 1..20) {
    $body = @{
        userId      = $userId
        productName = "Product $num"
        quantity    = 1
        price       = 99.99
    } | ConvertTo-Json

    try {
        $result = Invoke-RestMethod `
            -Uri "$OrderServiceUrl/api/orders" `
            -Method POST `
            -Body $body `
            -ContentType "application/json"

        Write-Host "  ✓ Created order $num (ID: $($result.id))" -ForegroundColor Green
        $orderCount++
    } catch {
        Write-Host "  ✗ Failed to create order $num" -ForegroundColor Red
        $orderErrors++
    }
}

Write-Host ""
Write-Host "Order Service Results: $orderCount created, $orderErrors failed" `
    -ForegroundColor ($(if ($orderErrors -eq 0) { "Green" } else { "Yellow" }))

# -------------------------------
# Statistics
# -------------------------------
Write-Host ""
Write-Host "Getting statistics..." -ForegroundColor Yellow

try {
    $userStats = Invoke-RestMethod "$UserServiceUrl/api/users/stats"
    Write-Host "User Stats:" -ForegroundColor Gray
    Write-Host "  Total Users   : $($userStats.totalUsers)"
    Write-Host "  Active Users  : $($userStats.activeUsers)"
    Write-Host "  Inactive Users: $($userStats.inactiveUsers)"
} catch {
    Write-Host "✗ Failed to get user stats" -ForegroundColor Red
}

try {
    $orderStats = Invoke-RestMethod "$OrderServiceUrl/api/orders/stats"
    Write-Host "Order Stats:" -ForegroundColor Gray
    Write-Host "  Total Revenue: $($orderStats.totalRevenue)"
    Write-Host "  Pending      : $($orderStats.pending)"
    Write-Host "  Processing   : $($orderStats.processing)"
    Write-Host "  Shipped      : $($orderStats.shipped)"
    Write-Host "  Delivered    : $($orderStats.delivered)"
} catch {
    Write-Host "✗ Failed to get order stats" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TEST COMPLETE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Metrics:" -ForegroundColor Yellow
Write-Host "  Prometheus: http://localhost:9090"
Write-Host "  Grafana   : http://localhost:3000 (admin/admin)"
Write-Host "  Jaeger    : http://localhost:16686"