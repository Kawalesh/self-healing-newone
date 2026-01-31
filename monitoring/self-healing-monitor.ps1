# Self-Healing Monitor Script
# Monitors services and automatically restarts them if they're down
# This script works with Docker Compose to provide self-healing capabilities

param(
    [int]$CheckInterval = 30,  # Check every 30 seconds
    [string]$ComposeFile = "docker-compose.yml"
)

$services = @("user-service", "order-service", "postgres-user", "postgres-order", "prometheus", "grafana", "jaeger", "alertmanager")

Write-Host "🔍 Self-Healing Monitor Started" -ForegroundColor Green
Write-Host "Monitoring services: $($services -join ', ')" -ForegroundColor Cyan
Write-Host "Check interval: $CheckInterval seconds" -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host ""

while ($true) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$timestamp] Checking service health..." -ForegroundColor Gray
    
    foreach ($service in $services) {
        try {
            # Check if container is running
            $container = docker ps --filter "name=$service" --format "{{.Names}} {{.Status}}" 2>$null
            
            if ($null -eq $container -or $container -eq "") {
                Write-Host "  ❌ $service is DOWN - Attempting restart..." -ForegroundColor Red
                
                # Restart the service
                docker-compose -f $ComposeFile restart $service 2>&1 | Out-Null
                
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "  ✅ $service restarted successfully" -ForegroundColor Green
                } else {
                    Write-Host "  ⚠️  Failed to restart $service" -ForegroundColor Yellow
                }
            } else {
                # Check if container is healthy (if health check exists)
                $status = docker inspect --format='{{.State.Health.Status}}' $service 2>$null
                
                if ($status -eq "unhealthy") {
                    Write-Host "  ⚠️  $service is UNHEALTHY - Attempting restart..." -ForegroundColor Yellow
                    docker-compose -f $ComposeFile restart $service 2>&1 | Out-Null
                } elseif ($status -eq "healthy" -or $status -eq "") {
                    # Service is healthy or doesn't have health check
                    Write-Host "  ✅ $service is UP" -ForegroundColor Green
                }
            }
        } catch {
            Write-Host "  ❌ Error checking $service : $_" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    Start-Sleep -Seconds $CheckInterval
}



