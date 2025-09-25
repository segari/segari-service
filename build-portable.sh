#!/bin/bash

echo "Building Segari Printer Middleware..."
echo

# Clean and package the application
echo "[1/2] Cleaning and packaging application..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Error: Failed to package application"
    exit 1
fi

# Build portable executable
echo
echo "[2/2] Creating portable executable..."
mvn verify -Pportable-exe -DskipTests
if [ $? -ne 0 ]; then
    echo "Error: Failed to create portable executable"
    exit 1
fi

echo
echo "Build completed successfully!"
echo
echo "Output files:"
echo "- Portable App: target/dist/segari-printer-middleware/segari-printer-middleware"
echo