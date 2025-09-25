@echo off
echo Building Segari Printer Middleware for Windows...
echo.

:: Clean and package the application
echo [1/3] Cleaning and packaging application...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo Error: Failed to package application
    pause
    exit /b 1
)

:: Build Windows installer (.exe)
echo.
echo [2/3] Creating Windows installer...
call mvn verify -Pwindows-exe -DskipTests
if errorlevel 1 (
    echo Error: Failed to create Windows installer
    pause
    exit /b 1
)

:: Build portable executable
echo.
echo [3/3] Creating portable executable...
call mvn verify -Pportable-exe -DskipTests
if errorlevel 1 (
    echo Error: Failed to create portable executable
    pause
    exit /b 1
)

echo.
echo Build completed successfully!
echo.
echo Output files:
echo - Windows Installer: target\dist\segari-printer-middleware-1.0.0.exe
echo - Portable App: target\dist\segari-printer-middleware\segari-printer-middleware.exe
echo.
pause