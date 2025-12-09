# PowerShell script to test all services
Write-Host " Testing Cloud Native Observability Platform Services..." -ForegroundColor Green

# Function to test HTTP endpoint
function Test-ServiceEndpoint {
    param(
        [string]$Url,
        [string]$ServiceName
    )
    
    try {
        $response = Invoke-WebRequest -Uri $Url -TimeoutSec 10
        if ($response.StatusCode -eq 200) {
            Write-Host " $ServiceName is healthy" -ForegroundColor Green
            return $true
        } else {
            Write-Host " $ServiceName returned status code: $($response.StatusCode)" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host " $ServiceName is not responding: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Wait for services to start
Write-Host "Waiting for services to start (30 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Test all services
$services = @(
    @{ Url = "http://localhost:8081/health"; Name = "User Service" },
    @{ Url = "http://localhost:8082/health"; Name = "Order Service" },
    @{ Url = "http://localhost:8083/health"; Name = "AI Anomaly Detection" },
    @{ Url = "http://localhost:9090"; Name = "Prometheus" },
    @{ Url = "http://localhost:3000"; Name = "Grafana" },
    @{ Url = "http://localhost:16686"; Name = "Jaeger" }
)

$healthyServices = 0
$totalServices = $services.Count

foreach ($service in $services) {
    if (Test-ServiceEndpoint -Url $service.Url -ServiceName $service.Name) {
        $healthyServices++
    }
}

Write-Host ""
Write-Host " Test Results: $healthyServices/$totalServices services are healthy" -ForegroundColor Cyan

if ($healthyServices -eq $totalServices) {
    Write-Host " All services are running successfully!" -ForegroundColor Green
} else {
    Write-Host " Some services are not responding. Check Docker logs:" -ForegroundColor Yellow
    Write-Host "  docker-compose logs" -ForegroundColor White
}

# Test AI anomaly detection API
Write-Host ""
Write-Host " Testing AI Anomaly Detection API..." -ForegroundColor Cyan

try {
    $testData = @{
        timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
        service_name = "test-service"
        cpu_usage = 75.5
        memory_usage = 512
        response_time = 150
        request_count = 1000
        error_rate = 0.02
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "http://localhost:8083/detect" -Method Post -Body $testData -ContentType "application/json"
    Write-Host " AI Anomaly Detection API is working" -ForegroundColor Green
   Write-Host ('   Anomaly Score: ' + $response.anomaly_score) -ForegroundColor White
Write-Host ('   Is Anomaly: ' + $response.is_anomaly) -ForegroundColor White

} catch {
  Write-Host (' AI Anomaly Detection API test failed: ' + $_.Exception.Message) -ForegroundColor Red

}
