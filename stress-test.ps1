param(
    [int]$Duration = 60,
    [int]$Concurrency = 10,
    [ValidateSet("user", "order", "all")]
    [string]$TargetService = "all"
)

# ===============================
# SAFE GLOBAL SETTINGS
# ===============================
$ErrorActionPreference = "Stop"
$ConfirmPreference = "None"

# ===============================
# SERVICE URLS
# ===============================
$UserServiceUrl  = "http://localhost:8081"
$OrderServiceUrl = "http://localhost:8082"

# ===============================
# SAFE COLOR OUTPUT
# ===============================
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )

    if (-not [System.Enum]::IsDefined([System.ConsoleColor], $Color)) {
        $Color = "White"
    }

    Write-Host $Message -ForegroundColor $Color
}

# ===============================
# HEADER
# ===============================
Write-ColorOutput "========================================" "Cyan"
Write-ColorOutput "CLOUD NATIVE OBSERVABILITY STRESS TEST" "Cyan"
Write-ColorOutput "========================================" "Cyan"
Write-ColorOutput "Duration: $Duration seconds" "White"
Write-ColorOutput "Concurrency: $Concurrency" "White"
Write-ColorOutput "Target Service: $TargetService" "White"
Write-ColorOutput "========================================" "Cyan"
Write-Host ""

# ===============================
# HEALTH CHECKS
# ===============================
Write-ColorOutput "Checking services..." "Yellow"

if ($TargetService -in @("user", "all")) {
    try {
        Invoke-WebRequest `
            -Uri "$UserServiceUrl/actuator/health" `
            -UseBasicParsing `
            -TimeoutSec 5 | Out-Null
        Write-ColorOutput "✓ User Service OK" "Green"
    } catch {
        Write-ColorOutput "✗ User Service DOWN" "Red"
        exit 1
    }
}

if ($TargetService -in @("order", "all")) {
    try {
        Invoke-WebRequest `
            -Uri "$OrderServiceUrl/actuator/health" `
            -UseBasicParsing `
            -TimeoutSec 5 | Out-Null
        Write-ColorOutput "✓ Order Service OK" "Green"
    } catch {
        Write-ColorOutput "✗ Order Service DOWN" "Red"
        exit 1
    }
}

Write-Host ""

# ===============================
# REQUEST FUNCTIONS
# ===============================
function Call-UserService {
    try {
        Invoke-RestMethod `
            -Uri "$UserServiceUrl/api/users" `
            -Method GET `
            -UseBasicParsing `
            -TimeoutSec 10 | Out-Null
    } catch {}
}

function Call-OrderService {
    try {
        Invoke-RestMethod `
            -Uri "$OrderServiceUrl/api/orders" `
            -Method GET `
            -UseBasicParsing `
            -TimeoutSec 10 | Out-Null
    } catch {}
}

# ===============================
# STRESS LOOP (SAFE)
# ===============================
Write-ColorOutput "Starting stress test..." "Yellow"

$endTime = (Get-Date).AddSeconds($Duration)

while ((Get-Date) -lt $endTime) {

    for ($i = 1; $i -le $Concurrency; $i++) {

        switch ($TargetService) {
            "user"  { Call-UserService }
            "order" { Call-OrderService }
            "all"   {
                Call-UserService
                Call-OrderService
            }
        }
    }

    Start-Sleep -Milliseconds 200
}

# ===============================
# FINISH
# ===============================
Write-Host ""
Write-ColorOutput "========================================" "Cyan"
Write-ColorOutput "STRESS TEST COMPLETED SUCCESSFULLY" "Green"
Write-ColorOutput "========================================" "Cyan"
Write-Host ""
Write-ColorOutput "Observability URLs:" "Yellow"
Write-ColorOutput "Prometheus : http://localhost:9090" "White"
Write-ColorOutput "Grafana    : http://localhost:3000 (admin/admin)" "White"
Write-ColorOutput "Jaeger     : http://localhost:16686" "White"
Write-Host ""