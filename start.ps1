# PowerShell script to start the platform
Write-Host "🚀 Starting Cloud Native Observability Platform..." -ForegroundColor Green

# Start all services with Docker Compose
Write-Host "Starting services with Docker Compose..." -ForegroundColor Yellow
docker-compose up --build

Write-Host "Platform started successfully!" -ForegroundColor Green
