@echo off
echo ========================================
echo Quick Stress Test - Cloud Native Observability
echo ========================================
echo.

echo Testing User Service...
echo Creating 5 users...
for /L %%i in (1,1,5) do (
    curl.exe -X POST http://localhost:8081/api/users ^
        -H "Content-Type: application/json" ^
        -d "{\"username\":\"user%%i\",\"email\":\"user%%i@test.com\",\"firstName\":\"User\",\"lastName\":\"%%i\"}"
    echo.
)

echo.
echo Getting all users...
curl.exe http://localhost:8081/api/users
echo.

echo.
echo Testing Order Service...
echo Getting all orders...
curl.exe http://localhost:8082/api/orders
echo.

echo.
echo Getting order stats...
curl.exe http://localhost:8082/api/orders/stats
echo.

echo.
echo Getting user stats...
curl.exe http://localhost:8081/api/users/stats
echo.

echo.
echo ========================================
echo Quick test completed!
echo ========================================
pause

