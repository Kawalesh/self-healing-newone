# PowerShell setup script for Windows
Write-Host "🚀 Setting up Cloud Native Observability Platform..." -ForegroundColor Green

# Check if Docker is running
Write-Host "Checking Docker status..." -ForegroundColor Yellow
try {
    docker version | Out-Null
    Write-Host "✅ Docker is running" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}

# Check if Java is installed
Write-Host "Checking Java installation..." -ForegroundColor Yellow
try {
    $javaVersion = java --version 2>&1 | Select-String "21"
    if ($javaVersion) {
        Write-Host "✅ Java 21 is installed" -ForegroundColor Green
    } else {
        Write-Host "❌ Java 21 not found. Please install Java 21." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Java not found. Please install Java 21." -ForegroundColor Red
    exit 1
}

# Check if Maven is installed
Write-Host "Checking Maven installation..." -ForegroundColor Yellow
try {
    mvn --version | Out-Null
    Write-Host "✅ Maven is installed" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Maven not found. Installing Maven..." -ForegroundColor Yellow
    # Try to install Maven using Chocolatey
    if (Get-Command choco -ErrorAction SilentlyContinue) {
        choco install maven -y
    } else {
        Write-Host "❌ Please install Maven manually or install Chocolatey first." -ForegroundColor Red
        exit 1
    }
}

# Check if Node.js is installed
Write-Host "Checking Node.js installation..." -ForegroundColor Yellow
try {
    node --version | Out-Null
    Write-Host "✅ Node.js is installed" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Node.js not found. Installing Node.js..." -ForegroundColor Yellow
    if (Get-Command choco -ErrorAction SilentlyContinue) {
        choco install nodejs -y
    } else {
        Write-Host "❌ Please install Node.js manually." -ForegroundColor Red
        exit 1
    }
}

# Build Docker images
Write-Host "Building Docker images..." -ForegroundColor Yellow
docker-compose build

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Docker images built successfully" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to build Docker images" -ForegroundColor Red
    exit 1
}

# Install frontend dependencies
Write-Host "Installing frontend dependencies..." -ForegroundColor Yellow
Set-Location frontend-dashboard
npm install
Set-Location ..

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Frontend dependencies installed" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to install frontend dependencies" -ForegroundColor Red
    exit 1
}

Write-Host "🎉 Setup completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "To start the platform, run:" -ForegroundColor Cyan
Write-Host "  docker-compose up" -ForegroundColor White
Write-Host ""
Write-Host "Services will be available at:" -ForegroundColor Cyan
Write-Host "  User Service: http://localhost:8081" -ForegroundColor White
Write-Host "  Order Service: http://localhost:8082" -ForegroundColor White
Write-Host "  AI Anomaly Detection: http://localhost:8083" -ForegroundColor White
Write-Host "  Prometheus: http://localhost:9090" -ForegroundColor White
Write-Host "  Grafana: http://localhost:3000 (admin/admin)" -ForegroundColor White
Write-Host "  Jaeger: http://localhost:16686" -ForegroundColor White
