# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.6 printer middleware service built with Java. It's configured to use Spring Security and Spring Web, with virtual threads enabled for improved performance.

## Development Commands

### Build and Run
```bash
# Clean and compile the project
./mvnw clean compile

# Run the application
./mvnw spring-boot:run

# Package as JAR
./mvnw clean package

# Run tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=SegariPrinterMiddlewareApplicationTests

# Skip tests during build
./mvnw clean package -DskipTests
```

### Code Quality
```bash
# Check for dependency updates
./mvnw versions:display-dependency-updates

# Generate project info reports
./mvnw site
```

## Architecture

The application follows standard Spring Boot conventions:

- **Main Application**: `id.segari.printer.segariprintermiddleware.SegariPrinterMiddlewareApplication` - Entry point with `@SpringBootApplication`
- **Package Structure**: `id.segari.printer.segariprintermiddleware` base package with subdirectories:
  - `controller/` - REST endpoints (currently empty, to be implemented)
  - `service/` - Business logic layer (currently empty, to be implemented)
- **Configuration**: Uses `application.properties` with virtual threads enabled
- **Security**: Spring Security is included as a dependency but not yet configured

## Key Technologies

- **Java Version**: 24 (OpenJDK 24.0.2-tem via SDKMAN as specified in .sdkmanrc)
- **Spring Boot**: 3.5.6 with parent POM inheritance
- **Dependencies**: Spring Web, Spring Security, Spring Boot Test, Spring Security Test, Lombok, usb4java
- **Build Tool**: Maven with wrapper (mvnw)

## Testing

Tests use JUnit 5 (Jupiter) with Spring Boot Test annotations. The base test class `SegariPrinterMiddlewareApplicationTests` verifies the application context loads correctly.