# Test from host and from Docker network to find why "User not found" in Docker
# Run from project root: .\docker-check-user-service.ps1

Write-Host "=== 1. From your PC (localhost) ===" -ForegroundColor Cyan
try {
    $r = Invoke-RestMethod -Uri "http://localhost:8081/api/users/1/exists" -Method Get -ErrorAction Stop
    Write-Host "  GET localhost:8081/api/users/1/exists -> exists = $($r.exists)" -ForegroundColor Green
} catch {
    Write-Host "  FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== 2. What URL does order-service use? (check startup log) ===" -ForegroundColor Cyan
docker logs order-service 2>&1 | Select-String "UserServiceClient initialized"

Write-Host "`n=== 3. From inside Docker network (order-service calling user-service) ===" -ForegroundColor Cyan
$net = docker inspect order-service --format "{{range \$k, \$v := .NetworkSettings.Networks}}{{\$k}}{{end}}" 2>$null
if ($net) {
    Write-Host "  Calling user-service from network: $net"
    $out = docker run --rm --network $net curlimages/curl -s http://user-service:8081/api/users/1/exists 2>&1
    Write-Host "  Response: $out"
} else {
    Write-Host "  order-service container not found. Start stack first: docker compose up -d"
}

Write-Host "`n=== 4. If step 2 shows localhost:8081 -> rebuild order-service and set SPRING_PROFILES_ACTIVE=docker ===" -ForegroundColor Yellow
Write-Host "  docker compose build order-service && docker compose up -d order-service" -ForegroundColor Gray
