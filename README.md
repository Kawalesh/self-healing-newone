# Cloud Native Observability Platform

A comprehensive self-healing microservices platform with AI-powered anomaly detection, built with Spring Boot, Kubernetes, and modern observability tools.

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   User Service  │    │  Order Service  │    │ AI Anomaly Det. │
│   (Spring Boot) │    │  (Spring Boot)  │    │   (FastAPI)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌─────────────────────────────────────────────┐
         │            Observability Stack              │
         │  ┌─────────────┐ ┌─────────┐ ┌─────────────┐│
         │  │ Prometheus  │ │ Grafana │ │   Jaeger    ││
         │  │ (Metrics)   │ │(Viz.)   │ │ (Tracing)   ││
         │  └─────────────┘ └─────────┘ └─────────────┘│
         └─────────────────────────────────────────────┘
                                 │
         ┌─────────────────────────────────────────────┐
         │         Self-Healing Operator               │
         │     (Kubernetes Controller)                 │
         └─────────────────────────────────────────────┘
                                 │
         ┌─────────────────────────────────────────────┐
         │        React Dashboard                      │
         │     (Frontend Visualization)                │
         └─────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- Java 21 (OpenJDK)
- Docker & Docker Compose
- Node.js 18+ (for frontend)
- Minikube (for Kubernetes deployment)
- kubectl

### 1. Development Environment Setup

```bash
# Clone the repository
git clone <repository-url>
cd cloud-native-observability

# Verify Java installation
java --version

# Install Maven (if not already installed)
# On Windows with Chocolatey: choco install maven
# On macOS with Homebrew: brew install maven
# On Ubuntu: sudo apt-get install maven
```

### 2. Build and Run with Docker Compose

```bash
# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d

# Check service status
docker-compose ps
```

**Services will be available at:**
- User Service: http://localhost:8081
- Order Service: http://localhost:8082
- AI Anomaly Detection: http://localhost:8083
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Jaeger: http://localhost:16686

### 3. Kubernetes Deployment (Minikube)

```bash
# Start Minikube
minikube start

# Build Docker images in Minikube
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

### 4. Frontend Dashboard

```bash
cd frontend-dashboard

# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build
```

The dashboard now includes an operations console powered by React + Material UI:
- Live health checks for both Spring Boot services, along with mock metrics/anomaly widgets.
- User management (create/delete) wired directly to `user-service`.
- Order management (create/cancel) wired directly to `order-service`.

Default API targets are `http://localhost:8081` and `http://localhost:8082`.  
Override them by exporting:

```bash
set REACT_APP_USER_SERVICE_URL=http://user-service:8081
set REACT_APP_ORDER_SERVICE_URL=http://order-service:8082
```

Restart the React dev server after changing these environment variables so it picks up the new values.

## 📊 Features

### Core Microservices
- **User Service**: User management with metrics exposure
- **Order Service**: Order processing with distributed tracing
- Both services include:
  - Spring Boot Actuator endpoints
  - Prometheus metrics
  - Jaeger distributed tracing
  - Health checks

### Observability Stack
- **Prometheus**: Metrics collection and storage
- **Grafana**: Visualization and alerting
- **Jaeger**: Distributed tracing
- Custom dashboards for system monitoring

### AI-Powered Anomaly Detection
- **Isolation Forest** algorithm for anomaly detection
- Real-time metrics analysis
- RESTful API for anomaly detection
- Integration with Prometheus metrics

### Self-Healing Capabilities
- **Kubernetes Operator** for automated healing
- Automatic pod restart on failures
- Dynamic scaling based on anomalies
- Traffic rerouting for unhealthy services

### Security Monitoring
- Suspicious traffic pattern detection
- IP-based anomaly detection
- API usage pattern analysis
- Integration with Grafana alerts

## 🔧 Configuration

### Environment Variables

```bash
# User Service
SPRING_PROFILES_ACTIVE=docker
SERVER_PORT=8081

# Order Service
SPRING_PROFILES_ACTIVE=docker
SERVER_PORT=8082

# AI Anomaly Detection
PROMETHEUS_URL=http://prometheus:9090

# Grafana
GF_SECURITY_ADMIN_PASSWORD=admin
```

### Prometheus Configuration

The Prometheus configuration (`monitoring/prometheus.yml`) includes:
- Service discovery for microservices
- Custom scrape intervals
- Metric path configuration

### Kubernetes Manifests

- Deployment configurations with resource limits
- Service definitions for internal communication
- ConfigMaps for configuration management
- RBAC for self-healing operator

## 📈 Monitoring and Alerting

### Key Metrics Tracked
- CPU and Memory usage
- Request latency and throughput
- Error rates
- Custom business metrics

### Grafana Dashboards
- Service health overview
- Performance metrics visualization
- Anomaly detection alerts
- Resource utilization trends

### Alerting Rules
- High CPU/Memory usage
- Service unavailability
- Anomaly detection alerts
- Custom business logic alerts

## 🤖 AI/ML Components

### Anomaly Detection Model
- **Algorithm**: Isolation Forest (unsupervised)
- **Features**: CPU, Memory, Response Time, Request Count, Error Rate
- **Training**: Historical metrics data
- **Prediction**: Real-time anomaly scoring

### Model Training
```bash
# Train the model with historical data
curl -X POST http://localhost:8083/train \
  -H "Content-Type: application/json" \
  -d @training_data.json
```

### Anomaly Detection
```bash
# Detect anomalies in real-time
curl -X POST http://localhost:8083/detect \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": 1640995200,
    "service_name": "user-service",
    "cpu_usage": 85.5,
    "memory_usage": 512,
    "response_time": 250,
    "request_count": 1000,
    "error_rate": 0.05
  }'
```

## 🔄 Self-Healing Mechanisms

### Automatic Recovery
- Failed pod restart
- Service scaling based on load
- Traffic rerouting
- Health check monitoring

### Healing Policies
- CPU threshold-based scaling
- Memory leak detection
- Response time monitoring
- Error rate threshold alerts

## 🧪 Testing

### Unit Tests
```bash
# Run Spring Boot tests
mvn test

# Run AI service tests
cd ai-anomaly-detection
python -m pytest tests/
```

### Integration Tests
```bash
# Test service endpoints
curl http://localhost:8081/health
curl http://localhost:8082/health
curl http://localhost:8083/health
```

### Load Testing
```bash
# Use tools like Apache Bench or Artillery
ab -n 1000 -c 10 http://localhost:8081/hello
```

## 📝 API Documentation

### User Service Endpoints
- `GET /health` - Health check
- `GET /hello` - Simple greeting
- `GET /actuator/prometheus` - Prometheus metrics
- `GET /actuator/health` - Detailed health info

### Order Service Endpoints
- `GET /health` - Health check
- `GET /hello` - Simple greeting
- `GET /actuator/prometheus` - Prometheus metrics
- `GET /actuator/health` - Detailed health info

### AI Anomaly Detection Endpoints
- `GET /health` - Service health
- `POST /train` - Train anomaly detection model
- `POST /detect` - Detect anomalies in metrics
- `GET /fetch-metrics` - Fetch Prometheus metrics
- `GET /metrics` - Prometheus metrics

## 🛠️ Development

### Project Structure
```
cloud-native-observability/
├── user-service/           # User management microservice
├── order-service/          # Order processing microservice
├── ai-anomaly-detection/   # AI/ML anomaly detection service
├── frontend-dashboard/     # React dashboard
├── monitoring/            # Prometheus & Grafana configs
├── k8s/                   # Kubernetes manifests
├── docker-compose.yml     # Local development setup
└── README.md             # This file
```

### Adding New Services
1. Create new Spring Boot application
2. Add Prometheus metrics and Jaeger tracing
3. Create Dockerfile and Kubernetes manifests
4. Update docker-compose.yml
5. Configure Prometheus scraping

### Contributing
1. Fork the repository
2. Create feature branch
3. Add tests for new functionality
4. Submit pull request

## 📚 Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Support

For questions and support:
- Create an issue in the repository
- Check the documentation
- Review the troubleshooting guide

---

**Built with ❤️ for cloud-native observability and self-healing systems**
