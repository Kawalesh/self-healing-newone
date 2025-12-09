# PowerShell script for stress testing the platform
param(
    [int]$Duration = 300,  # Duration in seconds (default 5 minutes)
    [int]$Concurrency = 10,  # Number of concurrent requests
    [string]$TargetService = "all"  # Target service: user, order, ai, or all
)

Write-Host " Starting Stress Test..." -ForegroundColor Red
Write-Host "Duration: $Duration seconds" -ForegroundColor Yellow
Write-Host "Concurrency: $Concurrency requests" -ForegroundColor Yellow
Write-Host "Target: $TargetService" -ForegroundColor Yellow

# Function to make HTTP requests
function Invoke-StressTest {
    param(
        [string]$Url,
        [string]$ServiceName,
        [int]$Duration,
        [int]$Concurrency
    )
    
    $endTime = (Get-Date).AddSeconds($Duration)
    $requestCount = 0
    $errorCount = 0
    $totalResponseTime = 0
    
    Write-Host "Testing $ServiceName at $Url" -ForegroundColor Cyan
    
    while ((Get-Date) -lt $endTime) {
        $jobs = @()
        
        # Start concurrent requests
        for ($i = 0; $i -lt $Concurrency; $i++) {
            $jobs += Start-Job -ScriptBlock {
                param($Url)
                $startTime = Get-Date
                try {
                    $response = Invoke-WebRequest -Uri $Url -TimeoutSec 10 -UseBasicParsing
                    $endTime = Get-Date
                    $responseTime = ($endTime - $startTime).TotalMilliseconds
                    
                    return @{
                        Success = $true
                        StatusCode = $response.StatusCode
                        ResponseTime = $responseTime
                        Error = $null
                    }
                } catch {
                    $endTime = Get-Date
                    $responseTime = ($endTime - $startTime).TotalMilliseconds
                    
                    return @{
                        Success = $false
                        StatusCode = 0
                        ResponseTime = $responseTime
                        Error = $_.Exception.Message
                    }
                }
            } -ArgumentList $Url
        }
        
        # Wait for all jobs to complete
        $jobs | Wait-Job | Out-Null
        
        # Collect results
        foreach ($job in $jobs) {
            $result = Receive-Job -Job $job
            Remove-Job -Job $job
            
            $requestCount++
            $totalResponseTime += $result.ResponseTime
            
            if (-not $result.Success) {
                $errorCount++
                Write-Host "Error: $($result.Error)" -ForegroundColor Red
            }
        }
        
        # Brief pause to prevent overwhelming the system
        Start-Sleep -Milliseconds 100
    }
    
    $avgResponseTime = if ($requestCount -gt 0) { $totalResponseTime / $requestCount } else { 0 }
    $errorRate = if ($requestCount -gt 0) { ($errorCount / $requestCount) * 100 } else { 0 }
    
    Write-Host "`n $ServiceName Results:" -ForegroundColor Green
    Write-Host "  Total Requests: $requestCount" -ForegroundColor White
    Write-Host "  Errors: $errorCount" -ForegroundColor White
    Write-Host "  Error Rate: $([math]::Round($errorRate, 2))%" -ForegroundColor White
    Write-Host "  Avg Response Time: $([math]::Round($avgResponseTime, 2))ms" -ForegroundColor White
    Write-Host "  Requests/sec: $([math]::Round($requestCount / $Duration, 2))" -ForegroundColor White
    
    return @{
        ServiceName = $ServiceName
        TotalRequests = $requestCount
        Errors = $errorCount
        ErrorRate = $errorRate
        AvgResponseTime = $avgResponseTime
        RequestsPerSecond = $requestCount / $Duration
    }
}

# Define test targets
$testTargets = @()

switch ($TargetService.ToLower()) {
    "user" {
        $testTargets += @{ Url = "http://localhost:8081/hello"; Name = "User Service" }
    }
    "order" {
        $testTargets += @{ Url = "http://localhost:8082/hello"; Name = "Order Service" }
    }
    "ai" {
        $testTargets += @{ Url = "http://localhost:8083/health"; Name = "AI Service" }
    }
    "all" {
        $testTargets += @{ Url = "http://localhost:8081/hello"; Name = "User Service" }
        $testTargets += @{ Url = "http://localhost:8082/hello"; Name = "Order Service" }
        $testTargets += @{ Url = "http://localhost:8083/health"; Name = "AI Service" }
    }
    default {
        Write-Host "Invalid target service. Use: user, order, ai, or all" -ForegroundColor Red
        exit 1
    }
}

# Check if services are running
Write-Host "`n Checking service availability..." -ForegroundColor Yellow
foreach ($target in $testTargets) {
    try {
        $response = Invoke-WebRequest -Uri $target.Url -TimeoutSec 5 -UseBasicParsing
        Write-Host " $($target.Name) is responding" -ForegroundColor Green
    } catch {
        Write-Host " $($target.Name) is not responding: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# Run stress tests
Write-Host "`n Starting stress tests..." -ForegroundColor Green
$results = @()

foreach ($target in $testTargets) {
    $result = Invoke-StressTest -Url $target.Url -ServiceName $target.Name -Duration $Duration -Concurrency $Concurrency
    $results += $result
}

# Summary
Write-Host "`n Stress Test Summary:" -ForegroundColor Magenta
Write-Host "=" * 50 -ForegroundColor White

$totalRequests = ($results | Measure-Object -Property TotalRequests -Sum).Sum
$totalErrors = ($results | Measure-Object -Property Errors -Sum).Sum
$avgErrorRate = if ($totalRequests -gt 0) { ($totalErrors / $totalRequests) * 100 } else { 0 }
$avgResponseTime = ($results | Measure-Object -Property AvgResponseTime -Average).Average
$totalRPS = ($results | Measure-Object -Property RequestsPerSecond -Sum).Sum

Write-Host "Total Requests: $totalRequests" -ForegroundColor White
Write-Host "Total Errors: $totalErrors" -ForegroundColor White
Write-Host "Overall Error Rate: $([math]::Round($avgErrorRate, 2))%" -ForegroundColor White
Write-Host "Average Response Time: $([math]::Round($avgResponseTime, 2))ms" -ForegroundColor White
Write-Host "Total Requests/sec: $([math]::Round($totalRPS, 2))" -ForegroundColor White

# Performance analysis
Write-Host "`n Performance Analysis:" -ForegroundColor Cyan

if ($avgErrorRate -lt 1) {
    Write-Host " Excellent error rate (< 1%)" -ForegroundColor Green
} elseif ($avgErrorRate -lt 5) {
    Write-Host " Good error rate (< 5%)" -ForegroundColor Yellow
} else {
    Write-Host " High error rate (> 5%) - system may be overloaded" -ForegroundColor Red
}

if ($avgResponseTime -lt 100) {
    Write-Host " Excellent response time (< 100ms)" -ForegroundColor Green
} elseif ($avgResponseTime -lt 500) {
    Write-Host " Good response time (< 500ms)" -ForegroundColor Yellow
} else {
    Write-Host " Slow response time (> 500ms) - performance issues detected" -ForegroundColor Red
}

if ($totalRPS -gt 100) {
    Write-Host " High throughput (> 100 req/sec)" -ForegroundColor Green
} elseif ($totalRPS -gt 50) {
    Write-Host " Moderate throughput (> 50 req/sec)" -ForegroundColor Yellow
} else {
    Write-Host " Low throughput (< 50 req/sec) - scalability issues" -ForegroundColor Red
}

Write-Host "`n Stress test completed!" -ForegroundColor Green
