# Segari Printer Middleware

A Spring Boot printer middleware service for handling print operations with Zebra printers and USB communication.

## Requirements

- Java 24 (OpenJDK 24.0.2-tem via SDKMAN)
- Maven 3.6+

## Build Instructions

### Standard Build
```bash
# Clean and compile
./mvnw clean compile

# Package as JAR
./mvnw clean package

# Skip tests during build
./mvnw clean package -DskipTests
```

### Building Windows Executable

#### Option 1: Windows Installer (.exe)
```bash
# Build with Windows executable profile
./mvnw clean package -Pwindows-exe

# The installer will be created in: target/dist/
```

#### Option 2: Portable Executable (no installer)
```bash
# Build portable executable
./mvnw clean package -Pportable-exe

# The portable app will be created in: target/dist/
```

**Note**: Windows executable builds require:
- Running on Windows or having cross-platform jpackage support
- JDK 14+ with jpackage tool available

### Running the Application

#### Development Mode
```bash
./mvnw spring-boot:run
```

#### Production JAR
```bash
java -jar target/segari-printer-middleware-0.0.1-SNAPSHOT.jar
```

#### Windows Executable
Double-click the generated `.exe` file or run from command line.

## Auto-Update System

The application includes a built-in auto-update mechanism that can automatically download and install new versions.

### How Auto-Update Works

1. **Update Check**: The application periodically checks for updates from a configured update server
2. **Version Comparison**: Compares current version with the latest available version
3. **Download**: If an update is available, it downloads the new executable to `~/.segari-printer/updates/`
4. **Installation**: Creates a batch script that:
   - Backs up the current executable
   - Replaces it with the new version
   - Restarts the application
   - Cleans up temporary files

### Configuration

Configure auto-updates in `application.properties`:

```properties
# Enable/disable update checking
update.check.enabled=true

# Update server URL
update.server.url=https://your-update-server.com/api

# Current app version (automatically set from pom.xml)
app.version=1.0.0
```

### Update Server API

The update server should provide these endpoints:

- `GET /version?current={version}` - Returns version information:
```json
{
  "currentVersion": "1.0.0",
  "latestVersion": "1.1.0",
  "updateAvailable": true,
  "downloadUrl": "https://server.com/download/v1.1.0/app.exe",
  "releaseNotes": "Bug fixes and improvements"
}
```

### Manual Update Process

1. Check for updates via the application UI or API
2. Download will happen automatically if update is available
3. Apply update when prompted (application will restart)
4. Backup of previous version is kept as `.backup`

### Update Storage

- Updates are downloaded to: `%USERPROFILE%\.segari-printer\updates\`
- Backup files are created alongside the main executable
- Update scripts are temporary and self-delete after completion

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=SegariPrinterMiddlewareApplicationTests
```

## Dependencies

- Spring Boot 3.5.6
- Spring Security
- Spring Web & WebSocket
- Zebra SDK API 2.14.5198
- USB4Java 1.3.0
- Jackson for JSON processing

## Architecture

- **Base Package**: `id.segari.printer.segariprintermiddleware`
- **Controllers**: REST endpoints for API access
- **Services**: Business logic including update management
- **Configuration**: Virtual threads enabled for performance