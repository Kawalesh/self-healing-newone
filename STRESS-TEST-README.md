# Stress Test - How to Run

## ⚠️ Important: Running from CMD (Command Prompt)

**CMD cannot run PowerShell scripts directly!** You need to use one of these methods:

### Method 1: Use the Batch File (Easiest)
```cmd
stress-test-simple.cmd
```

### Method 2: Call PowerShell from CMD
```cmd
powershell -ExecutionPolicy Bypass -File .\stress-test-simple.ps1
```

### Method 3: Use PowerShell Terminal
Open PowerShell and run:
```powershell
.\stress-test-simple.ps1
```

## 📋 Available Stress Test Scripts

### 1. **stress-test-simple.ps1** - Quick Test
- Creates 10 users
- Creates 20 orders
- Shows statistics
- **Best for quick testing**

**Run from CMD:**
```cmd
powershell -ExecutionPolicy Bypass -File .\stress-test-simple.ps1
```

**Or use the batch file:**
```cmd
stress-test-simple.cmd
```

### 2. **stress-test.ps1** - Full Load Test
- Configurable duration and concurrency
- Comprehensive statistics
- Tests all endpoints
- **Best for performance testing**

**Run from CMD:**
```cmd
powershell -ExecutionPolicy Bypass -File .\stress-test.ps1 -Duration 60 -Concurrency 10
```

**Run from PowerShell:**
```powershell
.\stress-test.ps1 -Duration 60 -Concurrency 10
```

### 3. **stress-test-quick.cmd** - Basic CMD Test
- Uses curl commands
- No PowerShell needed
- **Best for basic testing**

**Run from CMD:**
```cmd
stress-test-quick.cmd
```

## 🎯 Quick Start Guide

### Step 1: Make sure services are running
```cmd
docker-compose ps
```

If not running:
```cmd
docker-compose up -d
```

### Step 2: Run the simple stress test
```cmd
stress-test-simple.cmd
```

**OR from PowerShell:**
```powershell
.\stress-test-simple.ps1
```

### Step 3: Watch the output
You'll see:
- ✓ Green checkmarks for successful operations
- ✗ Red X for failures
- Statistics at the end

## 📊 What You'll See

The stress test will show:
1. **Service availability check** - Verifies services are running
2. **User creation** - Creates 10 test users
3. **Order creation** - Creates 20 orders
4. **Statistics** - Shows user and order stats
5. **Links to monitoring tools** - Prometheus, Grafana, Jaeger

## 🔍 Monitoring During Test

While the test runs, you can monitor:

1. **Grafana Dashboard**: http://localhost:3000
   - Login: admin/admin
   - View real-time metrics

2. **Prometheus**: http://localhost:9090
   - Query metrics directly

3. **Service Logs**:
```cmd
docker-compose logs -f user-service
docker-compose logs -f order-service
```

## ❓ Troubleshooting

### "File opens in editor instead of running"
- **Solution**: Use `powershell -ExecutionPolicy Bypass -File .\stress-test-simple.ps1` from CMD
- **OR**: Use the `.cmd` file: `stress-test-simple.cmd`

### "Execution Policy Error"
- **Solution**: Use `-ExecutionPolicy Bypass` flag:
```cmd
powershell -ExecutionPolicy Bypass -File .\stress-test-simple.ps1
```

### "Service not available"
- **Solution**: Start services first:
```cmd
docker-compose up -d
```

### "No output visible"
- **Solution**: Make sure you're running from the project root directory
- Check that PowerShell is installed: `powershell --version`

## 💡 Tips

1. **Start with simple test**: Use `stress-test-simple.cmd` first
2. **Check services**: Always verify services are running before testing
3. **Watch logs**: Keep another terminal open with `docker-compose logs -f`
4. **Monitor metrics**: Open Grafana in browser to see real-time metrics

## 📝 Example Output

```
========================================
QUICK STRESS TEST
========================================

Checking services...
✓ User Service is available
✓ Order Service is available

Testing User Service...
Creating 10 users...
  ✓ Created user 1 (ID: 1)
  ✓ Created user 2 (ID: 2)
  ...

User Service Results: 10 created, 0 failed

Testing Order Service...
  Found 10 users
  Creating 20 orders for user ID: 1...
  ✓ Created order 1 (ID: 1)
  ...

Order Service Results: 20 created, 0 failed

Getting statistics...
  User Stats:
    Total Users: 10
    Active Users: 5
  Order Stats:
    Total Revenue: 1999.80
    Pending: 15
    ...

========================================
TEST COMPLETE
========================================
```

