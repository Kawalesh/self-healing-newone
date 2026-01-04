@echo off
echo ========================================
echo Quick Stress Test - Cloud Native Observability
echo ========================================
echo.
echo Running PowerShell script...
echo.

powershell -ExecutionPolicy Bypass -File "%~dp0stress-test-simple.ps1"

echo.
echo Press any key to exit...
pause >nul

