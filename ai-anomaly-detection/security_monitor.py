import requests
import json
import time
import logging
from typing import Dict, List, Any
from collections import defaultdict, deque
import numpy as np
from sklearn.ensemble import IsolationForest

logger = logging.getLogger(__name__)

class SecurityMonitor:
    """Security-aware monitoring for detecting suspicious traffic patterns"""
    
    def __init__(self):
        self.ip_request_counts = defaultdict(int)
        self.ip_request_times = defaultdict(deque)
        self.user_agents = defaultdict(int)
        self.suspicious_patterns = []
        self.security_model = IsolationForest(contamination=0.05, random_state=42)
        self.is_trained = False
        
        # Security thresholds
        self.max_requests_per_minute = 100
        self.max_requests_per_ip_per_minute = 50
        self.suspicious_user_agents = [
            'sqlmap', 'nmap', 'nikto', 'dirb', 'gobuster', 'wfuzz',
            'burp', 'zap', 'scanner', 'bot', 'crawler', 'spider'
        ]
        
    def analyze_request(self, request_data: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze a single request for security threats"""
        current_time = time.time()
        client_ip = request_data.get('client_ip', 'unknown')
        user_agent = request_data.get('user_agent', '')
        endpoint = request_data.get('endpoint', '')
        method = request_data.get('method', 'GET')
        status_code = request_data.get('status_code', 200)
        
        threats = []
        risk_score = 0.0
        
        # Track request counts
        self.ip_request_counts[client_ip] += 1
        self.ip_request_times[client_ip].append(current_time)
        
        # Clean old requests (older than 1 minute)
        minute_ago = current_time - 60
        while (self.ip_request_times[client_ip] and 
               self.ip_request_times[client_ip][0] < minute_ago):
            self.ip_request_times[client_ip].popleft()
        
        # Check for suspicious user agents
        user_agent_lower = user_agent.lower()
        for suspicious_ua in self.suspicious_user_agents:
            if suspicious_ua in user_agent_lower:
                threats.append(f"Suspicious user agent detected: {suspicious_ua}")
                risk_score += 0.3
                break
        
        # Check for high request rate from single IP
        recent_requests = len(self.ip_request_times[client_ip])
        if recent_requests > self.max_requests_per_ip_per_minute:
            threats.append(f"High request rate from IP: {recent_requests} requests/min")
            risk_score += 0.4
        
        # Check for suspicious endpoints
        suspicious_endpoints = [
            '/admin', '/wp-admin', '/phpmyadmin', '/.env', '/config',
            '/api/v1/admin', '/internal', '/debug', '/test'
        ]
        for suspicious_endpoint in suspicious_endpoints:
            if suspicious_endpoint in endpoint.lower():
                threats.append(f"Suspicious endpoint access: {endpoint}")
                risk_score += 0.2
                break
        
        # Check for HTTP methods abuse
        if method not in ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']:
            threats.append(f"Unusual HTTP method: {method}")
            risk_score += 0.1
        
        # Check for error rate patterns
        if status_code >= 400:
            error_key = f"{client_ip}:{endpoint}"
            if not hasattr(self, 'error_counts'):
                self.error_counts = defaultdict(int)
            self.error_counts[error_key] += 1
            
            if self.error_counts[error_key] > 10:
                threats.append(f"High error rate for {client_ip} on {endpoint}")
                risk_score += 0.3
        
        # ML-based anomaly detection
        if self.is_trained and recent_requests > 0:
            features = np.array([[
                recent_requests,
                len(user_agent),
                len(endpoint),
                status_code,
                risk_score
            ]])
            
            try:
                anomaly_score = self.security_model.decision_function(features)[0]
                is_anomaly = self.security_model.predict(features)[0] == -1
                
                if is_anomaly:
                    threats.append(f"ML-detected security anomaly (score: {anomaly_score:.3f})")
                    risk_score += 0.5
            except Exception as e:
                logger.error(f"Error in ML security analysis: {e}")
        
        return {
            'threats': threats,
            'risk_score': min(risk_score, 1.0),
            'is_threat': risk_score > 0.5,
            'client_ip': client_ip,
            'timestamp': current_time,
            'user_agent': user_agent,
            'endpoint': endpoint,
            'method': method,
            'status_code': status_code
        }
    
    def train_security_model(self, historical_data: List[Dict[str, Any]]) -> bool:
        """Train the security anomaly detection model"""
        try:
            features = []
            for data in historical_data:
                feature_vector = [
                    data.get('requests_per_minute', 0),
                    data.get('user_agent_length', 0),
                    data.get('endpoint_length', 0),
                    data.get('status_code', 200),
                    data.get('risk_score', 0.0)
                ]
                features.append(feature_vector)
            
            if len(features) < 10:
                logger.warning("Insufficient data for security model training")
                return False
            
            X = np.array(features)
            self.security_model.fit(X)
            self.is_trained = True
            
            logger.info(f"Security model trained with {len(features)} samples")
            return True
            
        except Exception as e:
            logger.error(f"Error training security model: {e}")
            return False
    
    def get_security_summary(self) -> Dict[str, Any]:
        """Get a summary of current security status"""
        current_time = time.time()
        minute_ago = current_time - 60
        
        # Count active IPs
        active_ips = sum(1 for times in self.ip_request_times.values() 
                        if times and times[-1] > minute_ago)
        
        # Count high-risk IPs
        high_risk_ips = sum(1 for ip, times in self.ip_request_times.items()
                           if len(times) > self.max_requests_per_ip_per_minute)
        
        return {
            'active_ips': active_ips,
            'high_risk_ips': high_risk_ips,
            'total_requests_tracked': sum(self.ip_request_counts.values()),
            'model_trained': self.is_trained,
            'suspicious_patterns_count': len(self.suspicious_patterns)
        }
    
    def reset_counters(self):
        """Reset all counters (useful for testing or periodic cleanup)"""
        self.ip_request_counts.clear()
        for ip in self.ip_request_times:
            self.ip_request_times[ip].clear()
        self.user_agents.clear()
        self.suspicious_patterns.clear()
        if hasattr(self, 'error_counts'):
            self.error_counts.clear()

# Global security monitor instance
security_monitor = SecurityMonitor()
