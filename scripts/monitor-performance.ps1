# PowerShell script for monitoring platform performance
param(
    [int]$Interval = 30,  # Monitoring interval in seconds
    [string]$OutputFile = "performance-metrics.csv"
)

Write-Host "📊 Starting Performance Monitoring..." -ForegroundColor Green
Write-Host "Interval: $Interval seconds" -ForegroundColor Yellow
Write-Host "Output File: $OutputFile" -ForegroundColor Yellow

# Create CSV header
$csvHeader = "Timestamp,Service,ResponseTime,StatusCode,Error,CPU,Memory,ActiveConnections"
$csvHeader | Out-File -FilePath $OutputFile -Encoding UTF8

# Function to get service metrics
function Get-ServiceMetrics {
    param([string]$ServiceUrl, [string]$ServiceName)
    
    $metrics = @{
        Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        Service = $ServiceName
        ResponseTime = 0
        StatusCode = 0
        Error = ""
        CPU = 0
        Memory = 0
        ActiveConnections = 0
    }
    
    try {
        $startTime = Get-Date
        $response = Invoke-WebRequest -Uri $ServiceUrl -TimeoutSec 10 -UseBasicParsing
        $endTime = Get-Date
        $metrics.ResponseTime = ($endTime - $startTime).TotalMilliseconds
        $metrics.StatusCode = $response.StatusCode
    } catch {
        $metrics.Error = $_.Exception.Message
        $metrics.StatusCode = 0
    }
    
    # Get system metrics (simplified)
    try {
        $processes = Get-Process | Where-Object { $_.ProcessName -like "*java*" -or $_.ProcessName -like "*python*" }
        $metrics.CPU = ($processes | Measure-Object -Property CPU -Sum).Sum
        $metrics.Memory = ($processes | Measure-Object -Property WorkingSet -Sum).Sum / 1MB
    } catch {
        $metrics.CPU = 0
        $metrics.Memory = 0
    }
    
    return $metrics
}

# Function to get Docker container stats
function Get-DockerStats {
    try {
        $stats = docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" | ConvertFrom-Csv -Delimiter "`t"
        return $stats
    } catch {
        return @()
    }
}

# Function to write metrics to CSV
function Write-MetricsToCSV {
    param([array]$Metrics, [string]$FilePath)
    
    foreach ($metric in $Metrics) {
        $csvLine = "$($metric.Timestamp),$($metric.Service),$($metric.ResponseTime),$($metric.StatusCode),$($metric.Error),$($metric.CPU),$($metric.Memory),$($metric.ActiveConnections)"
        $csvLine | Add-Content -Path $FilePath -Encoding UTF8
    }
}

# Define services to monitor
$services = @(
    @{ Url = "http://localhost:8081/health"; Name = "User Service" },
    @{ Url = "http://localhost:8082/health"; Name = "Order Service" },
    @{ Url = "http://localhost:8083/health"; Name = "AI Service" },
    @{ Url = "http://localhost:9090"; Name = "Prometheus" },
    @{ Url = "http://localhost:3000"; Name = "Grafana" }
)

Write-Host "`n🔍 Monitoring services:" -ForegroundColor Cyan
foreach ($service in $services) {
    Write-Host "  - $($service.Name): $($service.Url)" -ForegroundColor White
}

# Main monitoring loop
$startTime = Get-Date
Write-Host "`n⏰ Started monitoring at $($startTime.ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor Green

try {
    while ($true) {
        $currentTime = Get-Date
        Write-Host "`n📊 Collecting metrics at $($currentTime.ToString('HH:mm:ss'))..." -ForegroundColor Yellow
        
        $allMetrics = @()
        
        # Collect metrics for each service
        foreach ($service in $services) {
            $metrics = Get-ServiceMetrics -ServiceUrl $service.Url -ServiceName $service.Name
            $allMetrics += $metrics
            
            $status = if ($metrics.Error -eq "") { "✅" } else { "❌" }
            Write-Host "  $status $($service.Name): $($metrics.ResponseTime)ms" -ForegroundColor White
        }
        
        # Get Docker stats
        $dockerStats = Get-DockerStats
        if ($dockerStats.Count -gt 0) {
            Write-Host "`n🐳 Docker Container Stats:" -ForegroundColor Cyan
            foreach ($stat in $dockerStats) {
                Write-Host "  $($stat.Container): CPU $($stat.CPUPerc), Memory $($stat.MemUsage)" -ForegroundColor White
            }
        }
        
        # Write metrics to CSV
        Write-MetricsToCSV -Metrics $allMetrics -FilePath $OutputFile
        
        # Calculate and display summary
        $healthyServices = ($allMetrics | Where-Object { $_.Error -eq "" }).Count
        $avgResponseTime = ($allMetrics | Where-Object { $_.ResponseTime -gt 0 } | Measure-Object -Property ResponseTime -Average).Average
        
        Write-Host "`n📈 Summary:" -ForegroundColor Magenta
        Write-Host "  Healthy Services: $healthyServices/$($services.Count)" -ForegroundColor White
        Write-Host "  Avg Response Time: $([math]::Round($avgResponseTime, 2))ms" -ForegroundColor White
        Write-Host "  Total Memory Usage: $([math]::Round(($allMetrics | Measure-Object -Property Memory -Sum).Sum, 2))MB" -ForegroundColor White
        
        # Wait for next interval
        Write-Host "`n⏳ Waiting $Interval seconds for next measurement..." -ForegroundColor Yellow
        Start-Sleep -Seconds $Interval
    }
} catch {
    Write-Host "`n🛑 Monitoring stopped: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n📄 Metrics saved to: $OutputFile" -ForegroundColor Green
Write-Host "🏁 Performance monitoring completed!" -ForegroundColor Green
