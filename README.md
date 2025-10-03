# Segari Service

A Spring Boot service for managing USB printers (Zebra, XPrinter) with WebSocket support for real-time print job reception, concurrent print queue management, and automatic update capabilities.

## Requirements

- Java 25 (OpenJDK via SDKMAN as specified in .sdkmanrc)
- Maven 3.6+
- **WiX Toolset v3** (required only for Windows installer builds)

## Build Instructions

### Standard JAR Build
```bash
# Clean and compile
./mvnw clean compile

# Package as JAR
./mvnw clean package

# Skip tests during build
./mvnw clean package -DskipTests
```

### Building Windows Executable

#### Portable Executable (no installer)
```bash
# Build portable executable with production profile
./mvnw clean verify -Pportable-exe -Dspring.profiles.active=prod

# The portable app will be created in: target/dist/segari-service/
```

#### Windows Installer (.exe)
```bash
# Build Windows installer with production profile
# REQUIRES: WiX Toolset v3 installed and in PATH
./mvnw clean verify -Pwindows-exe -Dspring.profiles.active=prod

# The installer will be created in: target/dist/
```

**Requirements for Windows builds**:
- JDK 14+ with jpackage tool available
- **WiX Toolset v3** (https://wixtoolset.org/releases/) - required for `.exe` installer only
- Windows OS or cross-platform jpackage support

### Running the Application

#### Development Mode
```bash
./mvnw spring-boot:run
```

#### With Spring Profile
```bash
# Run with production configuration
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=prod

# Run with theta configuration
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=theta
```

#### Production JAR
```bash
# Default profile
java -jar target/segari-service-0.0.1-SNAPSHOT.jar

# With specific profile
java -Dspring.profiles.active=prod -jar target/segari-service-0.0.1-SNAPSHOT.jar
```

#### Windows Executable
Double-click the generated `.exe` file or run from command line. The Spring profile is configured during build time via `-Dspring.profiles.active=prod`.

## API Endpoints

The application exposes the following REST API endpoints on port `54124`:

### Printer Management (`/v1/printer`)
- `GET /ping` - Health check endpoint
- `GET /connected/{id}` - Check if printer is connected
- `GET /usb` - List all available USB printers
- `POST /connect` - Connect to a USB printer
- `POST /print` - Submit a print job (adds to queue)
- `DELETE /disconnect/{id}` - Disconnect printer
- `GET /print-domain` - Get configured print domain URL

### Queue Management (`/v1/queue`)
- `GET /status` - Get overall queue status (all printers)
- `GET /status/{id}` - Get queue status for specific printer
- `GET /list/{id}` - List pending jobs for printer
- `DELETE /clear/{id}` - Clear queue for specific printer
- `DELETE /clear` - Clear all queues

### WebSocket Management (`/v1/websocket`)
- `POST /connect/{warehouseId}` - Connect to WebSocket server for warehouse
- `POST /disconnect/{warehouseId}` - Disconnect WebSocket
- `GET /status/{warehouseId}` - Get WebSocket connection status

### Update Management (`/v1/update`)
- `GET /check` - Check for available updates
- `POST /download` - Download update
- `GET /status` - Check if update is downloaded
- `POST /apply` - Apply downloaded update
- `GET /version` - Get current application version

### System Information (`/v1/versions`)
- `GET /` - Get Java runtime version information

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=SegariServiceApplicationTests
```

## Configuration

### Spring Profiles

The application supports multiple profiles for different environments:

- **Default** (`application.properties`) - Development settings
- **Production** (`application-prod.properties`) - Production environment
  - Backend: `https://api-v2.segari.id`
  - WebSocket: `wss://api-v2.segari.id`
- **Theta** (`application-theta.properties`) - Theta environment

### Key Configuration Properties

```properties
# Server
server.port=54124
spring.threads.virtual.enabled=true

# Application metadata
app.version=1.0.9
app.name=Segari Service

# WebSocket
websocket.server.url=wss://your-server.com
websocket.topic.print=/broker/warehouse-printers
websocket.reconnect.interval.ms=2000

# Backend API
segari.backend.endpoint=https://api-v2.segari.id

# Update system
update.check.enabled=true

# Graceful shutdown
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=5s
```

## Features

### USB Printer Support
- **Supported Vendors**: Zebra (0x0A5F), XPrinter (0x2D37)
- **Protocol**: ZPL (Zebra Programming Language)
- **Connection**: USB4Java library for direct USB communication
- **Multi-printer**: Support for multiple printers simultaneously
- **Auto-detection**: Automatically detects USB printers by vendor ID and device class

### Print Queue System
- **Per-Printer Queues**: Separate queue for each connected printer
- **Concurrency**: Virtual threads for high-performance processing
- **Limits**: Maximum 20 queues, 20 jobs per queue
- **Management**: Clear individual or all queues via API

### WebSocket Client
- **Protocol**: STOMP over WebSocket
- **Auto-reconnect**: Automatic reconnection with configurable interval
- **Warehouse-based**: One WebSocket connection per warehouse
- **Real-time**: Receives print jobs in real-time from server

### Auto-Update System
- **Version checking**: Checks for updates from backend API
- **Download**: Downloads update packages (.zip)
- **Self-update**: Automatically replaces executable and restarts
- **Windows batch script**: Creates and executes update script with admin privileges
- **Rollback support**: Keeps backup of previous version

### Performance
- **Virtual threads**: Enabled for improved concurrency (Spring Boot 3.5.6, Java 25)
- **Graceful shutdown**: Properly closes USB connections and WebSocket
- **Resource cleanup**: `@PreDestroy` hooks for proper resource management

## Dependencies

- **Spring Boot 3.5.6** - Main framework
- **Spring Web** - REST API
- **Spring WebSocket** - WebSocket client support
- **Spring Validation** - Request validation
- **USB4Java 1.3.0** - USB communication library
- **Jackson** - JSON processing with JSR310 support
- **Apache HttpClient 5** - HTTP client for update downloads
- **SLF4J/Logback** - Logging

## Architecture

### Package Structure
```
id.segari.service/
├── common/                          # DTOs and common utilities
│   ├── dto/
│   │   ├── printer/                # Printer-related DTOs
│   │   ├── queue/                  # Queue status DTOs
│   │   ├── websocket/              # WebSocket DTOs
│   │   ├── update/                 # Update system DTOs
│   │   └── external/               # External API responses
│   ├── response/                   # Response wrappers
│   └── InternalResponseCode.java   # Response codes
├── config/                          # Spring configurations
│   ├── CorsConfig.java
│   ├── ExecutorServiceConfig.java
│   ├── JacksonConfig.java
│   ├── PrintQueueConfig.java
│   ├── RestTemplateConfig.java
│   └── WebSocketConfig.java
├── controller/                      # REST controllers
│   ├── PrinterController.java
│   ├── QueueController.java
│   ├── UpdateController.java
│   ├── VersionController.java
│   └── WebSocketController.java
├── service/                         # Service interfaces
│   ├── PrinterService.java
│   ├── PrintQueueService.java
│   ├── UpdateService.java
│   ├── UrlService.java
│   └── WebSocketService.java
├── service/impl/                    # Service implementations
│   ├── zpl_printer/                # USB printer implementation
│   │   ├── ZplPrinterServiceImpl.java
│   │   ├── Printer.java
│   │   └── PrinterDescription.java
│   ├── print_queue/                # Queue implementation
│   │   ├── PrintQueueServiceImpl.java
│   │   └── ConsumerThreadRun.java
│   ├── websocket/                  # WebSocket client
│   │   ├── WebSocketServiceImpl.java
│   │   ├── PrintStompFrameHandler.java
│   │   └── PrintStompSessionHandlerAdapter.java
│   ├── url/                        # URL service
│   │   └── UrlServiceImpl.java
│   └── UpdateServiceImpl.java      # Update service
└── exception/                       # Exception handling
    ├── BaseException.java
    ├── InternalBaseException.java
    └── GlobalExceptionHandler.java
```

### Key Components

- **ZplPrinterServiceImpl**: Manages USB printer connections using USB4Java, supports vendor-specific detection
- **PrintQueueServiceImpl**: Concurrent queue management with virtual threads, one consumer thread per printer
- **WebSocketServiceImpl**: STOMP WebSocket client with automatic reconnection and session management
- **UpdateServiceImpl**: Handles version checking, update downloads, extraction, and self-update process
- **GlobalExceptionHandler**: Centralized exception handling for consistent API responses