# Self-Healing Architecture

This document describes the self-healing capabilities implemented in the cloud-native observability platform.

## Overview

The system implements multiple layers of self-healing to ensure high availability and automatic recovery from failures:

1. **Docker Restart Policies** - Automatic container restart on failure
2. **Circuit Breakers** - Resilience patterns for inter-service communication
3. **Health Checks** - Continuous monitoring of service health
4. **Prometheus Alerting** - Alert-based monitoring and notification
5. **Manual Monitoring Script** - Optional script-based service recovery

## Components

### 1. Docker Restart Policies

All services in `docker-compose.yml` are configured with `restart: unless-stopped`, which means:
- Containers automatically restart if they crash
- Containers restart if the Docker daemon restarts
- Containers do NOT restart if manually stopped

**Services with restart policies:**
- `user-service`
- `order-service`
- `postgres-user`
- `postgres-order`
- `prometheus`
- `grafana`
- `jaeger`
- `alertmanager`

### 2. Health Checks

Health checks are configured for all critical services:

**User Service:**
```yaml
healthcheck:
  test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

**Order Service:**
```yaml
healthcheck:
  test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

**PostgreSQL:**
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres"]
  interval: 10s
  timeout: 5s
  retries: 5
```

### 3. Circuit Breakers (Resilience4j)

The order-service uses Resilience4j circuit breakers to handle failures when calling the user-service:

**Configuration:**
- **Sliding Window Size:** 10 calls
- **Minimum Calls:** 5 before circuit can open
- **Failure Rate Threshold:** 50%
- **Wait Duration:** 10 seconds in OPEN state
- **Auto Transition:** Enabled (automatically tries HALF_OPEN state)

**Benefits:**
- Prevents cascading failures
- Fast failure response when user-service is down
- Automatic recovery when user-service comes back
- Metrics exported to Prometheus

**Circuit Breaker States:**
- **CLOSED:** Normal operation, requests pass through
- **OPEN:** Too many failures, requests fail fast
- **HALF_OPEN:** Testing if service recovered, limited requests allowed

### 4. Prometheus Alerting

Prometheus is configured with alert rules that trigger when services are down or experiencing issues.

**Alert Rules** (`monitoring/alert-rules.yml`):
- **ServiceDown:** Triggers when a service is down for >30 seconds
- **ServiceHighErrorRate:** Triggers when error rate >10% for 2 minutes
- **ServiceHighLatency:** Triggers when 95th percentile latency >1s for 5 minutes
- **DatabaseConnectionFailure:** Triggers when database connection fails
- **CircuitBreakerOpen:** Triggers when circuit breaker opens
- **HighMemoryUsage:** Triggers when memory usage >90% for 5 minutes
- **HighCPUUsage:** Triggers when CPU usage >80% for 5 minutes

**Alertmanager** (`monitoring/alertmanager.yml`):
- Routes alerts based on severity
- Groups related alerts
- Configurable webhook/email notifications

### 5. Monitoring Script

A PowerShell script (`monitoring/self-healing-monitor.ps1`) provides additional monitoring:

**Usage:**
```powershell
.\monitoring\self-healing-monitor.ps1
```

**Features:**
- Checks service health every 30 seconds (configurable)
- Automatically restarts unhealthy services
- Logs all actions with timestamps

## How It Works

### Automatic Recovery Flow

1. **Service Failure Detection:**
   - Health check fails OR
   - Container crashes OR
   - Prometheus detects service down

2. **Automatic Actions:**
   - Docker automatically restarts the container (restart policy)
   - Circuit breaker opens to prevent cascading failures
   - Alertmanager sends notifications (if configured)

3. **Recovery:**
   - Container restarts and health check passes
   - Circuit breaker transitions to HALF_OPEN, then CLOSED
   - Service resumes normal operation

### Manual Recovery

If automatic recovery doesn't work, you can manually restart services:

```bash
# Restart a specific service
docker-compose restart user-service

# Restart all services
docker-compose restart

# View service logs
docker logs user-service
docker logs order-service

# Check service health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## Monitoring and Observability

### View Circuit Breaker Status

```bash
# Check circuit breaker metrics in Prometheus
curl http://localhost:9090/api/v1/query?query=resilience4j_circuitbreaker_state

# View in Grafana
# Navigate to: http://localhost:3000
# Login: admin/admin
# Check the Observability Dashboard
```

### View Alerts

```bash
# View active alerts in Prometheus
curl http://localhost:9090/api/v1/alerts

# View alerts in Alertmanager
curl http://localhost:9093/api/v2/alerts
```

### Grafana Dashboard

The Grafana dashboard includes:
- Service health status
- Circuit breaker metrics
- Error rates and latency
- Resource usage (CPU, memory)

Access at: `http://localhost:3000` (admin/admin)

## Testing Self-Healing

### Test 1: Container Crash Recovery

```bash
# Stop a service container
docker stop user-service

# Wait 30 seconds - Docker should automatically restart it
docker ps | grep user-service

# Check logs
docker logs user-service
```

### Test 2: Circuit Breaker

```bash
# Stop user-service
docker stop user-service

# Make requests to order-service
curl http://localhost:8082/api/orders

# Circuit breaker should open after failures
# Check metrics:
curl http://localhost:8082/actuator/metrics/resilience4j.circuitbreaker.calls

# Restart user-service
docker start user-service

# Circuit breaker should automatically close after successful calls
```

### Test 3: Health Check Failure

```bash
# Simulate health check failure by stopping the service
docker stop user-service

# Health check will fail
# Docker will attempt to restart based on restart policy
```

## Configuration

### Adjusting Restart Policies

Edit `docker-compose.yml`:
```yaml
services:
  user-service:
    restart: always  # Always restart (even if manually stopped)
    # OR
    restart: unless-stopped  # Restart unless manually stopped (current)
    # OR
    restart: on-failure  # Only restart on failure
```

### Adjusting Circuit Breaker Settings

Edit `order-service/src/main/resources/application.yml`:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      userService:
        failureRateThreshold: 50  # Open at 50% failure rate
        waitDurationInOpenState: 10s  # Wait 10s before trying again
        slidingWindowSize: 10  # Evaluate last 10 calls
```

### Adjusting Alert Rules

Edit `monitoring/alert-rules.yml`:
```yaml
- alert: ServiceDown
  expr: up{job=~"user-service|order-service"} == 0
  for: 30s  # Alert after 30 seconds
```

## Best Practices

1. **Monitor Alerts:** Set up alert notifications (email, Slack, etc.) in Alertmanager
2. **Log Aggregation:** Use centralized logging (Loki) to track recovery events
3. **Capacity Planning:** Monitor resource usage to prevent OOM kills
4. **Graceful Shutdown:** Ensure services handle shutdown gracefully
5. **Database Backups:** Regular backups for database services
6. **Load Testing:** Test self-healing under load to ensure it works

## Troubleshooting

### Service Not Restarting

1. Check restart policy: `docker inspect user-service | grep RestartPolicy`
2. Check logs: `docker logs user-service`
3. Check Docker daemon: `docker info`
4. Manually restart: `docker-compose restart user-service`

### Circuit Breaker Stuck Open

1. Check user-service health: `curl http://localhost:8081/actuator/health`
2. Check circuit breaker metrics: `curl http://localhost:8082/actuator/metrics/resilience4j.circuitbreaker.state`
3. Manually reset (if needed): Restart order-service

### Alerts Not Firing

1. Check Prometheus targets: `http://localhost:9090/targets`
2. Check alert rules: `http://localhost:9090/alerts`
3. Check Alertmanager: `http://localhost:9093`
4. Verify service discovery: Services must be scraped by Prometheus

## Future Enhancements

- [ ] Kubernetes deployment with liveness/readiness probes
- [ ] Auto-scaling based on metrics
- [ ] Automated database failover
- [ ] Chaos engineering tests
- [ ] ML-based anomaly detection for proactive healing



