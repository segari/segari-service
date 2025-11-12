# Spring AOT Build Guide

## What Was Added

### 1. AOT Maven Profile
Added new `-Paot` profile to `pom.xml` that enables Ahead-of-Time compilation.

### 2. Optimization Properties
Added to `application.properties`:
- `spring.jmx.enabled=false` - Disables JMX for faster startup
- `spring.mvc.log-request-details=false` - Reduces logging overhead

## How to Use

### Standard Build (No AOT)
```bash
./mvnw clean package
```

### AOT-Optimized Build (Faster Startup)
```bash
./mvnw clean package -Paot
```

### AOT Build with Tests
```bash
./mvnw clean package -Paot
```

### AOT Build without Tests (Faster)
```bash
./mvnw clean package -Paot -DskipTests
```

## Running the AOT-Optimized Application

```bash
# Run the AOT-optimized JAR
java --enable-native-access=ALL-UNNAMED -jar target/segari-service-0.0.1-SNAPSHOT.jar
```

## Expected Benefits

### Startup Time Improvements
- **Standard Build**: ~3-5 seconds startup
- **AOT Build**: ~2-3.5 seconds startup (20-30% faster)

### Additional Optimizations Applied
✅ Virtual threads enabled
✅ JMX disabled
✅ Reduced logging overhead
✅ AOT pre-processing of Spring beans

## What AOT Does

1. **Bean Pre-Computation** - Spring beans are analyzed at build time
2. **Reflection Pre-Registration** - Reflection hints generated ahead of time
3. **Proxy Pre-Generation** - Dynamic proxies created during build
4. **Configuration Pre-Processing** - Configuration classes processed early

## When to Use Each Build

### Use Standard Build When:
- Development and debugging
- Frequent code changes
- Running tests extensively

### Use AOT Build When:
- Production deployments
- Windows .exe packaging
- Performance testing
- Final release builds

## Combining with Other Profiles

### AOT + Windows EXE
```bash
./mvnw clean package -Paot,windows-exe -Dspring.profiles.active=prod
```

### AOT + Portable
```bash
./mvnw clean package -Paot,portable-exe -Dspring.profiles.active=prod
```

## Troubleshooting

### If AOT Build Fails
1. Check for unsupported reflection patterns
2. Review native library compatibility (USB4Java, ZKTeco SDK)
3. Add runtime hints if needed (see Spring AOT documentation)

### Generated AOT Files Location
```
target/spring-aot/main/sources/
target/spring-aot/main/classes/
```

## Next Steps for Even Faster Startup

1. **GraalVM Native Image** (50-100ms startup)
   - Requires additional configuration
   - Not all libraries supported yet

2. **CRaC (Checkpoint/Restore)** (~10ms startup)
   - Requires CRaC-enabled JDK
   - Linux environment recommended

3. **Lazy Initialization** (Add to application.properties)
   ```properties
   spring.main.lazy-initialization=true
   ```
   Note: May cause first-request slowness

## Monitoring AOT Impact

### Measure Startup Time
```bash
# Standard build
time java --enable-native-access=ALL-UNNAMED -jar target/segari-service-0.0.1-SNAPSHOT.jar

# AOT build
time java --enable-native-access=ALL-UNNAMED -jar target/segari-service-0.0.1-SNAPSHOT.jar
```

### Check AOT Processing Logs
Look for:
- `spring-boot:process-aot` in build output
- Generated AOT source files in target/spring-aot/

## Additional Resources

- [Spring AOT Documentation](https://docs.spring.io/spring-framework/reference/core/aot.html)
- [Spring Boot AOT Processing](https://docs.spring.io/spring-boot/reference/packaging/aot.html)
- [Project Leyden](https://openjdk.org/projects/leyden/)
