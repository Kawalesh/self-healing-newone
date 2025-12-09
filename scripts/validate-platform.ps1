# Comprehensive platform validation script
Write-Host "🔍 Cloud Native Observability Platform Validation" -ForegroundColor Green
Write-Host "=" * 60 -ForegroundColor White

# Function to test HTTP endpoint
function Test-ServiceEndpoint {
    param(
        [string]$Url,
        [string]$ServiceName,
        [string]$ExpectedContent = ""
    )
    
    try {
        $response = Invoke-WebRequest -Uri $Url -TimeoutSec 10 -UseBasicParsing
        if ($response.StatusCode -eq 200) {
            if ($ExpectedContent -ne "" -and $response.Content -notlike "*$ExpectedContent*") {
                Write-Host "⚠️ $ServiceName responded but content doesn't match expected: $ExpectedContent" -ForegroundColor Yellow
                return $false
            }
            Write-Host "✅ $ServiceName is healthy" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ $ServiceName returned status code: $($response.StatusCode)" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "❌ $ServiceName is not responding: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Function to test API endpoint
function Test-ApiEndpoint {
    param(
        [string]$Url,
        [string]$Method = "GET",
        [string]$Body = "",
        [string]$ContentType = "application/json"
    )
    
    try {
        if ($Method -eq "POST" -and $Body -ne "") {
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Body $Body -ContentType $ContentType
        } else {
            $response = Invoke-RestMethod -Uri $Url -Method $Method
        }
        return $response
    } catch {
        Write-Host "API Error: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# Test 1: Basic Service Health
Write-Host "`n🏥 Testing Basic Service Health..." -ForegroundColor Cyan
$healthTests = @(
    @{ Url = "http://localhost:8081/health"; Name = "User Service"; Expected = "healthy" },
    @{ Url = "http://localhost:8082/health"; Name = "Order Service"; Expected = "healthy" },
    @{ Url = "http://localhost:8083/health"; Name = "AI Anomaly Detection"; Expected = "healthy" },
    @{ Url = "http://localhost:9090"; Name = "Prometheus" },
    @{ Url = "http://localhost:3000"; Name = "Grafana" },
    @{ Url = "http://localhost:16686"; Name = "Jaeger" }
)

$healthyServices = 0
foreach ($test in $healthTests) {
    if (Test-ServiceEndpoint -Url $test.Url -ServiceName $test.Name -ExpectedContent $test.Expected) {
        $healthyServices++
    }
}

Write-Host "Health Check Results: $healthyServices/$($healthTests.Count) services healthy" -ForegroundColor $(if ($healthyServices -eq $healthTests.Count) { "Green" } else { "Yellow" })

# Test 2: API Functionality
Write-Host "`n🔌 Testing API Functionality..." -ForegroundColor Cyan

# Test User Service API
Write-Host "Testing User Service API..." -ForegroundColor Yellow
$userResponse = Test-ApiEndpoint -Url "http://localhost:8081/hello"
if ($userResponse) {
    Write-Host "✅ User Service API working" -ForegroundColor Green
} else {
    Write-Host "❌ User Service API failed" -ForegroundColor Red
}

# Test Order Service API
Write-Host "Testing Order Service API..." -ForegroundColor Yellow
$orderResponse = Test-ApiEndpoint -Url "http://localhost:8082/hello"
if ($orderResponse) {
    Write-Host "✅ Order Service API working" -ForegroundColor Green
} else {
    Write-Host "❌ Order Service API failed" -ForegroundColor Red
}

# Test 3: AI Anomaly Detection
Write-Host "`n🤖 Testing AI Anomaly Detection..." -ForegroundColor Cyan

# Test anomaly detection API
$testData = @{
    timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    service_name = "test-service"
    cpu_usage = 75.5
    memory_usage = 512
    response_time = 150
    request_count = 1000
    error_rate = 0.02
} | ConvertTo-Json

$anomalyResponse = Test-ApiEndpoint -Url "http://localhost:8083/detect" -Method "POST" -Body $testData
if ($anomalyResponse) {
    Write-Host "✅ AI Anomaly Detection API working" -ForegroundColor Green
    Write-Host "  Anomaly Score: $($anomalyResponse.anomaly_score)" -ForegroundColor White
    Write-Host "  Is Anomaly: $($anomalyResponse.is_anomaly)" -ForegroundColor White
} else {
    Write-Host "❌ AI Anomaly Detection API failed" -ForegroundColor Red
}

# Test 4: Security Monitoring
Write-Host "`n🔒 Testing Security Monitoring..." -ForegroundColor Cyan

    $securityTestData = @{
        client_ip = "192.168.1.100"
        user_agent = "Mozilla/5.0"
        endpoint = "/api/users"
        method = "GET"
        status_code = 200
        timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    } | ConvertTo-Json

$securityResponse = Test-ApiEndpoint -Url "http://localhost:8083/security/analyze" -Method "POST" -Body $securityTestData
if ($securityResponse) {
    Write-Host "✅ Security Monitoring API working" -ForegroundColor Green
    Write-Host "  Risk Score: $($securityResponse.risk_score)" -ForegroundColor White
    Write-Host "  Is Threat: $($securityResponse.is_threat)" -ForegroundColor White
} else {
    Write-Host "❌ Security Monitoring API failed" -ForegroundColor Red
}

# Test 5: Prometheus Metrics
Write-Host "`n📊 Testing Prometheus Metrics..." -ForegroundColor Cyan

try {
    $prometheusResponse = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/query?query=up" -TimeoutSec 10
    if ($prometheusResponse.data.result.Count -gt 0) {
        Write-Host "✅ Prometheus is collecting metrics" -ForegroundColor Green
        Write-Host "  Active targets: $($prometheusResponse.data.result.Count)" -ForegroundColor White
    } else {
        Write-Host "⚠️ Prometheus is running but no metrics found" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Prometheus metrics query failed" -ForegroundColor Red
}

# Test 6: Docker Container Status
Write-Host "`n🐳 Checking Docker Container Status..." -ForegroundColor Cyan

try {
    $containers = docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | ConvertFrom-Csv -Delimiter "`t"
    if ($containers.Count -gt 0) {
        Write-Host "✅ Docker containers are running:" -ForegroundColor Green
        foreach ($container in $containers) {
            Write-Host "  $($container.Names): $($container.Status)" -ForegroundColor White
        }
    } else {
        Write-Host "❌ No Docker containers found" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Docker command failed" -ForegroundColor Red
}

# Test 7: Kubernetes Status (if available)
Write-Host "`n☸️ Checking Kubernetes Status..." -ForegroundColor Cyan

try {
    $kubectlVersion = kubectl version --client --short 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ kubectl is available" -ForegroundColor Green
        
        # Check if minikube is running
        $minikubeStatus = minikube status 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Minikube is running" -ForegroundColor Green
            
            # Check pods
            $pods = kubectl get pods --no-headers 2>$null
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✅ Kubernetes pods are accessible" -ForegroundColor Green
            }
        } else {
            Write-Host "⚠️ Minikube is not running" -ForegroundColor Yellow
        }
    } else {
        Write-Host "⚠️ kubectl is not available" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️ Kubernetes not available" -ForegroundColor Yellow
}

# Test 8: Frontend Dashboard
Write-Host "`n🖥️ Testing Frontend Dashboard..." -ForegroundColor Cyan

try {
    $frontendResponse = Invoke-WebRequest -Uri "http://localhost:3000" -TimeoutSec 10 -UseBasicParsing
    if ($frontendResponse.StatusCode -eq 200) {
        Write-Host "✅ Frontend Dashboard is accessible" -ForegroundColor Green
    } else {
        Write-Host "❌ Frontend Dashboard returned status: $($frontendResponse.StatusCode)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Frontend Dashboard is not accessible" -ForegroundColor Red
}

# Final Summary
Write-Host "`n📋 Validation Summary:" -ForegroundColor Magenta
Write-Host "=" * 40 -ForegroundColor White

$totalTests = 8
$passedTests = 0

if ($healthyServices -eq $healthTests.Count) { $passedTests++ }
if ($userResponse -and $orderResponse) { $passedTests++ }
if ($anomalyResponse) { $passedTests++ }
if ($securityResponse) { $passedTests++ }

Write-Host "Core Services Health: $healthyServices/$($healthTests.Count)" -ForegroundColor White
Write-Host "API Functionality: $(if ($userResponse -and $orderResponse) { 'PASS' } else { 'FAIL' })" -ForegroundColor White
Write-Host "AI Anomaly Detection: $(if ($anomalyResponse) { 'PASS' } else { 'FAIL' })" -ForegroundColor White
Write-Host "Security Monitoring: $(if ($securityResponse) { 'PASS' } else { 'FAIL' })" -ForegroundColor White

Write-Host "`n🎯 Platform Status: $(if ($healthyServices -eq $healthTests.Count -and $userResponse -and $orderResponse) { 'HEALTHY' } else { 'ISSUES DETECTED' })" -ForegroundColor $(if ($healthyServices -eq $healthTests.Count -and $userResponse -and $orderResponse) { 'Green' } else { 'Red' })

if ($healthyServices -eq $healthTests.Count -and $userResponse -and $orderResponse -and $anomalyResponse) {
    Write-Host "`n🎉 Platform validation completed successfully!" -ForegroundColor Green
    Write-Host "All core components are working correctly." -ForegroundColor Green
} else {
    Write-Host "`n⚠️ Some issues detected. Check the logs above for details." -ForegroundColor Yellow
    Write-Host "Run 'docker-compose logs' to see detailed error messages." -ForegroundColor Yellow
}

Write-Host "`n🚀 Next Steps:" -ForegroundColor Cyan
Write-Host "1. Access Grafana at http://localhost:3000 (admin/admin)" -ForegroundColor White
Write-Host "2. Access Jaeger at http://localhost:16686" -ForegroundColor White
Write-Host "3. Access Prometheus at http://localhost:9090" -ForegroundColor White
Write-Host "4. Run stress tests: .\scripts\stress-test.ps1" -ForegroundColor White
Write-Host "5. Monitor performance: .\scripts\monitor-performance.ps1" -ForegroundColor White
