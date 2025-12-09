# 🚀 Quick Start Guide

Get your Cloud Native Observability Platform up and running in minutes!

## Prerequisites

- ✅ Java 21 (already installed)
- ✅ Docker Desktop (install from [docker.com](https://docker.com))
- ✅ Node.js 18+ (install from [nodejs.org](https://nodejs.org))
- ✅ Maven (optional, for manual builds)

## 🏃‍♂️ Quick Start (5 minutes)

### 1. Setup Environment
```powershell
# Run the setup script
.\setup.ps1
```

### 2. Start Platform
```powershell
# Start all services
.\start.ps1

# Or manually with Docker Compose
docker-compose up --build
```

### 3. Validate Platform
```powershell
# Test all services
.\scripts\validate-platform.ps1
```

## 🌐 Access Your Platform

| Service | URL | Credentials |
|---------|-----|-------------|
| **User Service** | http://localhost:8081 | - |
| **Order Service** | http://localhost:8082 | - |
| **AI Anomaly Detection** | http://localhost:8083 | - |
| **Prometheus** | http://localhost:9090 | - |
| **Grafana** | http://localhost:3000 | admin/admin |
| **Jaeger** | http://localhost:16686 | - |
| **Frontend Dashboard** | http://localhost:3001 | - |

## 🧪 Test the Platform

### Basic Health Checks
```powershell
# Test individual services
curl http://localhost:8081/health
curl http://localhost:8082/health
curl http://localhost:8083/health
```

### AI Anomaly Detection Test
```powershell
# Test anomaly detection
$testData = @{
    timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    service_name = "test-service"
    cpu_usage = 85.5
    memory_usage = 512
    response_time = 250
    request_count = 1000
    error_rate = 0.05
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8083/detect" -Method Post -Body $testData -ContentType "application/json"
```

### Security Monitoring Test
```powershell
# Test security monitoring
$securityData = @{
    client_ip = "192.168.1.100"
    user_agent = "Mozilla/5.0"
    endpoint = "/api/users"
    method = "GET"
    status_code = 200
    timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8083/security/analyze" -Method Post -Body $securityData -ContentType "application/json"
```

## 📊 Monitoring & Observability

### Grafana Dashboards
1. Open http://localhost:3000
2. Login with admin/admin
3. Import dashboards for:
   - Service metrics
   - Anomaly detection
   - Security monitoring

### Jaeger Tracing
1. Open http://localhost:16686
2. Search for traces from your services
3. Analyze request flows

### Prometheus Metrics
1. Open http://localhost:9090
2. Query metrics:
   - `up` - Service availability
   - `http_requests_total` - Request counts
   - `http_request_duration_seconds` - Response times

## 🔥 Stress Testing

```powershell
# Run stress test (5 minutes, 10 concurrent requests)
.\scripts\stress-test.ps1 -Duration 300 -Concurrency 10

# Test specific service
.\scripts\stress-test.ps1 -TargetService "user" -Duration 60 -Concurrency 5
```

## 📈 Performance Monitoring

```powershell
# Monitor performance (30-second intervals)
.\scripts\monitor-performance.ps1 -Interval 30

# Monitor with custom output file
.\scripts\monitor-performance.ps1 -Interval 60 -OutputFile "my-metrics.csv"
```

## ☸️ Kubernetes Deployment

```powershell
# Start Minikube
minikube start

# Build images in Minikube
eval $(minikube docker-env)
docker-compose build

# Deploy to Kubernetes
kubectl apply -f k8s/

# Check deployments
kubectl get pods
kubectl get services

# Access services
minikube service grafana
minikube service jaeger
```

## 🛠️ Development

### Add New Microservice
1. Create new Spring Boot project
2. Add to parent `pom.xml`
3. Create Dockerfile
4. Add to `docker-compose.yml`
5. Create Kubernetes manifests

### Modify AI Models
1. Edit `ai-anomaly-detection/main.py`
2. Update `requirements.txt` if needed
3. Rebuild: `docker-compose build ai-anomaly-detection`

### Update Frontend
```powershell
cd frontend-dashboard
npm install
npm start  # Development
npm run build  # Production
```

## 🔧 Troubleshooting

### Services Not Starting
```powershell
# Check Docker status
docker ps
docker-compose logs

# Check specific service
docker-compose logs user-service
```

### Port Conflicts
```powershell
# Check what's using ports
netstat -ano | findstr :8081
netstat -ano | findstr :8082

# Kill process if needed
taskkill /PID <PID> /F
```

### Memory Issues
```powershell
# Check Docker resources
docker system df
docker system prune  # Clean up

# Increase Docker memory in Docker Desktop settings
```

## 📚 Learn More

- [Full Documentation](README.md)
- [API Documentation](docs/api.md)
- [Architecture Guide](docs/architecture.md)
- [Security Guide](docs/security.md)

## 🆘 Need Help?

1. Check the logs: `docker-compose logs`
2. Run validation: `.\scripts\validate-platform.ps1`
3. Check GitHub issues
4. Create new issue with logs

---

**🎉 You're ready to explore cloud-native observability!**
