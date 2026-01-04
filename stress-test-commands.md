# Stress Test Commands Guide

This guide provides various ways to stress test your microservices.

## Quick Start

### PowerShell Script (Recommended)
```powershell
# Basic stress test (60 seconds, 10 concurrent workers)
.\stress-test.ps1

# Custom duration and concurrency
.\stress-test.ps1 -Duration 120 -Concurrency 20

# Test only User Service
.\stress-test.ps1 -Duration 60 -Concurrency 10 -TargetService user

# Test only Order Service
.\stress-test.ps1 -Duration 60 -Concurrency 10 -TargetService order

# Verbose output
.\stress-test.ps1 -Duration 60 -Concurrency 5 -Verbose
```

### Simple Quick Test
```powershell
.\stress-test-simple.ps1
```

## Manual Commands

### Using curl (Windows PowerShell)
```powershell
# Create User
curl.exe -X POST http://localhost:8081/api/users `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"testuser\",\"email\":\"test@example.com\",\"firstName\":\"Test\",\"lastName\":\"User\"}'

# Get All Users
curl.exe http://localhost:8081/api/users

# Get User by ID
curl.exe http://localhost:8081/api/users/1

# Create Order
curl.exe -X POST http://localhost:8082/api/orders `
  -H "Content-Type: application/json" `
  -d '{\"userId\":1,\"productName\":\"Test Product\",\"quantity\":2,\"price\":99.99}'

# Get All Orders
curl.exe http://localhost:8082/api/orders
```

### Using Invoke-WebRequest (PowerShell)
```powershell
# Create User
$body = @{
    username = "testuser"
    email = "test@example.com"
    firstName = "Test"
    lastName = "User"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/api/users" -Method POST -Body $body -ContentType "application/json"

# Get All Users
Invoke-RestMethod -Uri "http://localhost:8081/api/users"

# Create Order
$orderBody = @{
    userId = 1
    productName = "Test Product"
    quantity = 2
    price = 99.99
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/orders" -Method POST -Body $orderBody -ContentType "application/json"
```

## Load Testing with Apache Bench (if installed)

```powershell
# Install Apache Bench (if not available)
# Download from: https://www.apachelounge.com/download/

# Test User Service GET endpoint
ab -n 1000 -c 10 http://localhost:8081/api/users

# Test Order Service GET endpoint
ab -n 1000 -c 10 http://localhost:8082/api/orders

# Test with POST (requires a file with JSON)
ab -n 500 -c 10 -p user.json -T application/json http://localhost:8081/api/users
```

## Continuous Load Test Script

Create a file `continuous-load.ps1`:

```powershell
$duration = 300  # 5 minutes
$interval = 1    # 1 second between requests
$endTime = (Get-Date).AddSeconds($duration)

while ((Get-Date) -lt $endTime) {
    # Random operations
    $rand = Get-Random -Minimum 1 -Maximum 4
    
    switch ($rand) {
        1 { Invoke-RestMethod -Uri "http://localhost:8081/api/users" }
        2 { Invoke-RestMethod -Uri "http://localhost:8082/api/orders" }
        3 { Invoke-RestMethod -Uri "http://localhost:8081/api/users/stats" }
        4 { Invoke-RestMethod -Uri "http://localhost:8082/api/orders/stats" }
    }
    
    Start-Sleep -Seconds $interval
}
```

## Monitoring During Stress Test

While running stress tests, monitor:

1. **Prometheus Metrics**: http://localhost:9090
   - Query: `http_server_requests_seconds_count`
   - Query: `jvm_memory_used_bytes`

2. **Grafana Dashboard**: http://localhost:3000
   - Login: admin/admin
   - View the Observability Dashboard

3. **Jaeger Traces**: http://localhost:16686
   - Search for traces from user-service and order-service

4. **Service Logs**:
```powershell
# User Service logs
docker-compose logs -f user-service

# Order Service logs
docker-compose logs -f order-service

# All services
docker-compose logs -f
```

## Expected Metrics

During stress testing, you should see:

- **Request Rate**: Requests per second
- **Response Time**: Average, P95, P99 latencies
- **Error Rate**: Percentage of failed requests
- **Throughput**: Successful requests per second
- **Memory Usage**: JVM heap usage
- **CPU Usage**: Service CPU consumption

## Tips

1. **Start Small**: Begin with low concurrency (5-10) and short duration (30-60s)
2. **Gradually Increase**: Monitor metrics and gradually increase load
3. **Watch for Errors**: Check error rates and response times
4. **Database Impact**: Monitor PostgreSQL connections and query performance
5. **Network**: Ensure Docker network can handle the load

## Troubleshooting

If services become unresponsive:

1. Check Docker container status:
```powershell
docker-compose ps
```

2. Check service health:
```powershell
curl.exe http://localhost:8081/actuator/health
curl.exe http://localhost:8082/actuator/health
```

3. Restart services:
```powershell
docker-compose restart user-service order-service
```

4. Check database connections:
```powershell
docker-compose logs postgres-user
docker-compose logs postgres-order
```

