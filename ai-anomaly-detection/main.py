from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
import pandas as pd
import numpy as np
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler
import requests
import json
from typing import List, Dict, Any
import logging
from prometheus_client import Counter, Histogram, Gauge, generate_latest, CONTENT_TYPE_LATEST
from fastapi.responses import Response
import asyncio
import time
from security_monitor import security_monitor

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="AI Anomaly Detection Service", version="1.0.0")

# Prometheus metrics
anomaly_counter = Counter('anomalies_detected_total', 'Total number of anomalies detected')
request_duration = Histogram('anomaly_detection_duration_seconds', 'Time spent on anomaly detection')
active_models = Gauge('active_models', 'Number of active ML models')
model_accuracy = Gauge('model_accuracy', 'Current model accuracy')

# Security metrics
security_threats_counter = Counter('security_threats_detected_total', 'Total number of security threats detected')
security_risk_score = Gauge('security_risk_score', 'Current security risk score')
active_ips_gauge = Gauge('active_ips', 'Number of active IP addresses')

class MetricsData(BaseModel):
    timestamp: float
    service_name: str
    cpu_usage: float
    memory_usage: float
    response_time: float
    request_count: int
    error_rate: float

class AnomalyResult(BaseModel):
    is_anomaly: bool
    anomaly_score: float
    metrics: Dict[str, float]
    timestamp: float

class SecurityRequest(BaseModel):
    client_ip: str
    user_agent: str
    endpoint: str
    method: str
    status_code: int
    timestamp: float

class SecurityResult(BaseModel):
    threats: List[str]
    risk_score: float
    is_threat: bool
    client_ip: str
    timestamp: float

# Global model and scaler
isolation_forest = None
scaler = StandardScaler()
training_data = []
is_model_trained = False

@app.on_event("startup")
async def startup_event():
    """Initialize the anomaly detection model on startup"""
    global isolation_forest
    isolation_forest = IsolationForest(contamination=0.1, random_state=42)
    active_models.set(1)
    logger.info("AI Anomaly Detection Service started")

@app.get("/")
async def root():
    return {"message": "AI Anomaly Detection Service", "status": "running"}

@app.get("/health")
async def health():
    return {"status": "healthy", "model_trained": is_model_trained}

@app.get("/metrics")
async def metrics():
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)

@app.post("/train")
async def train_model(data: List[MetricsData]):
    """Train the anomaly detection model with historical data"""
    global isolation_forest, scaler, training_data, is_model_trained
    
    try:
        # Convert to DataFrame
        df = pd.DataFrame([{
            'cpu_usage': d.cpu_usage,
            'memory_usage': d.memory_usage,
            'response_time': d.response_time,
            'request_count': d.request_count,
            'error_rate': d.error_rate
        } for d in data])
        
        # Scale the features
        scaled_data = scaler.fit_transform(df)
        
        # Train the model
        isolation_forest.fit(scaled_data)
        training_data = scaled_data.tolist()
        is_model_trained = True
        
        logger.info(f"Model trained with {len(data)} samples")
        return {"message": "Model trained successfully", "samples": len(data)}
        
    except Exception as e:
        logger.error(f"Error training model: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Training failed: {str(e)}")

@app.post("/detect", response_model=AnomalyResult)
async def detect_anomaly(data: MetricsData):
    """Detect anomalies in real-time metrics"""
    if not is_model_trained:
        raise HTTPException(status_code=400, detail="Model not trained yet")
    
    start_time = time.time()
    
    try:
        # Prepare features
        features = np.array([[
            data.cpu_usage,
            data.memory_usage,
            data.response_time,
            data.request_count,
            data.error_rate
        ]])
        
        # Scale features
        scaled_features = scaler.transform(features)
        
        # Predict anomaly
        anomaly_score = isolation_forest.decision_function(scaled_features)[0]
        is_anomaly = isolation_forest.predict(scaled_features)[0] == -1
        
        # Update metrics
        if is_anomaly:
            anomaly_counter.inc()
        
        # Record processing time
        processing_time = time.time() - start_time
        request_duration.observe(processing_time)
        
        return AnomalyResult(
            is_anomaly=bool(is_anomaly),
            anomaly_score=float(anomaly_score),
            metrics={
                'cpu_usage': data.cpu_usage,
                'memory_usage': data.memory_usage,
                'response_time': data.response_time,
                'request_count': data.request_count,
                'error_rate': data.error_rate
            },
            timestamp=time.time()
        )
        
    except Exception as e:
        logger.error(f"Error detecting anomaly: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Detection failed: {str(e)}")

@app.get("/fetch-metrics")
async def fetch_prometheus_metrics():
    """Fetch metrics from Prometheus and detect anomalies"""
    try:
        prometheus_url = "http://prometheus:9090/api/v1/query"
        
        # Query for recent metrics
        queries = [
            "rate(http_requests_total[5m])",
            "process_cpu_seconds_total",
            "jvm_memory_used_bytes",
            "http_request_duration_seconds"
        ]
        
        results = {}
        for query in queries:
            response = requests.get(prometheus_url, params={'query': query})
            if response.status_code == 200:
                data = response.json()
                results[query] = data.get('data', {}).get('result', [])
        
        return {"metrics": results, "timestamp": time.time()}
        
    except Exception as e:
        logger.error(f"Error fetching Prometheus metrics: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch metrics: {str(e)}")

@app.post("/security/analyze", response_model=SecurityResult)
async def analyze_security_request(request: SecurityRequest):
    """Analyze a request for security threats"""
    try:
        request_data = {
            'client_ip': request.client_ip,
            'user_agent': request.user_agent,
            'endpoint': request.endpoint,
            'method': request.method,
            'status_code': request.status_code
        }
        
        result = security_monitor.analyze_request(request_data)
        
        # Update metrics
        if result['is_threat']:
            security_threats_counter.inc()
        security_risk_score.set(result['risk_score'])
        
        return SecurityResult(**result)
        
    except Exception as e:
        logger.error(f"Error analyzing security request: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Security analysis failed: {str(e)}")

@app.post("/security/train")
async def train_security_model(data: List[SecurityRequest]):
    """Train the security anomaly detection model"""
    try:
        historical_data = []
        for req in data:
            historical_data.append({
                'requests_per_minute': 1,  # Simplified for demo
                'user_agent_length': len(req.user_agent),
                'endpoint_length': len(req.endpoint),
                'status_code': req.status_code,
                'risk_score': 0.0  # Will be calculated during analysis
            })
        
        success = security_monitor.train_security_model(historical_data)
        
        if success:
            return {"message": "Security model trained successfully", "samples": len(data)}
        else:
            raise HTTPException(status_code=400, detail="Failed to train security model")
            
    except Exception as e:
        logger.error(f"Error training security model: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Training failed: {str(e)}")

@app.get("/security/summary")
async def get_security_summary():
    """Get security monitoring summary"""
    try:
        summary = security_monitor.get_security_summary()
        
        # Update metrics
        active_ips_gauge.set(summary['active_ips'])
        
        return summary
        
    except Exception as e:
        logger.error(f"Error getting security summary: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to get security summary: {str(e)}")

@app.middleware("http")
async def security_middleware(request: Request, call_next):
    """Middleware to analyze requests for security threats"""
    start_time = time.time()
    
    # Extract request information
    client_ip = request.client.host
    user_agent = request.headers.get("user-agent", "")
    endpoint = str(request.url.path)
    method = request.method
    
    # Analyze the request
    request_data = {
        'client_ip': client_ip,
        'user_agent': user_agent,
        'endpoint': endpoint,
        'method': method,
        'status_code': 200  # Will be updated after response
    }
    
    security_result = security_monitor.analyze_request(request_data)
    
    # Log security threats
    if security_result['is_threat']:
        logger.warning(f"Security threat detected: {security_result}")
    
    # Process the request
    response = await call_next(request)
    
    # Update security analysis with actual status code
    request_data['status_code'] = response.status_code
    final_result = security_monitor.analyze_request(request_data)
    
    # Add security headers
    response.headers["X-Security-Risk-Score"] = str(final_result['risk_score'])
    if final_result['is_threat']:
        response.headers["X-Security-Threat"] = "true"
    
    return response

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8083)
