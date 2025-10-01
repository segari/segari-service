package id.segari.printer.segariprintermiddleware.service.impl;

import id.segari.printer.segariprintermiddleware.common.dto.external.AppVersion;
import id.segari.printer.segariprintermiddleware.common.dto.external.SegariResponse;
import id.segari.printer.segariprintermiddleware.common.dto.update.VersionInfo;
import id.segari.printer.segariprintermiddleware.service.UpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class UpdateServiceImpl implements UpdateService {
    private static final Logger logger = LoggerFactory.getLogger(UpdateServiceImpl.class);

    private final RestTemplate restTemplate;

    @Value("${app.version:1.0.0}")
    private String currentVersion;


    @Value("${segari.backend.endpoint}")
    private String updateServerUrl;

    @Value("${update.check.enabled:true}")
    private boolean updateCheckEnabled;

    private final Path updateDirectory;
    private final Path downloadedUpdatePath;
    private final Path extractedUpdatePath;

    public UpdateServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.updateDirectory = Paths.get(System.getProperty("user.home"), ".segari-printer", "updates");
        this.downloadedUpdatePath = updateDirectory.resolve("update.zip");
        this.extractedUpdatePath = updateDirectory.resolve("extracted");

        try {
            Files.createDirectories(updateDirectory);
            Files.createDirectories(extractedUpdatePath);
        } catch (IOException e) {
            logger.error("Failed to create update directory: {}", e.getMessage());
        }
    }

    @Override
    public VersionInfo checkForUpdates() {
        if (!updateCheckEnabled || updateServerUrl.isEmpty()) {
            logger.debug("Update checking is disabled or no update server configured");
            return new VersionInfo(currentVersion, currentVersion, false, "", "");
        }

        try {
            logger.debug("Checking for updates from: {}", updateServerUrl);

            SegariResponse<AppVersion> response = restTemplate.exchange(
                updateServerUrl + "/v1/warehouse-printers/printer-middleware/versions",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<SegariResponse<AppVersion>>() {}
            ).getBody();

            if (response != null && response.data() != null) {
                logger.info("Update check completed. Response: {}", response.message());
                // Parse response data to create VersionInfo - assuming response.data() contains version info
                return new VersionInfo(currentVersion, response.data().version(), isUpdateAvailable(currentVersion, response.data().version()), response.data().url(), response.message());
            }

        } catch (Exception e) {
            logger.error("Failed to check for updates: {}", e.getMessage());
        }

        return new VersionInfo(currentVersion, currentVersion, false, "", "Update check failed");
    }

    private boolean isUpdateAvailable(String currentVersion, String latestVersion) {
        try {
            String[] current = currentVersion.split("\\.");
            String[] latest = latestVersion.split("\\.");

            int length = Math.max(current.length, latest.length);

            for (int i = 0; i < length; i++) {
                int currentNum = i < current.length ? Integer.parseInt(current[i]) : 0;
                int latestNum = i < latest.length ? Integer.parseInt(latest[i]) : 0;

                if (latestNum > currentNum) {
                    return true;
                } else if (latestNum < currentNum) {
                    return false;
                }
            }

            return false;

        } catch (Exception e) {
            logger.warn("Error comparing versions: {} vs {}", currentVersion, latestVersion);
            return false;
        }
    }

    @Override
    public void downloadUpdate(String downloadUrl) {
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            throw new IllegalArgumentException("Download URL cannot be empty");
        }

        logger.info("Starting download of update from: {}", downloadUrl);
        logger.info("Download destination: {}", downloadedUpdatePath);

        try {
            // Ensure update directory exists
            if (!Files.exists(updateDirectory)) {
                logger.info("Creating update directory: {}", updateDirectory);
                Files.createDirectories(updateDirectory);
            }

            URL url = new URL(downloadUrl);
            logger.info("Opening connection to: {}", url);

            try (InputStream inputStream = url.openStream()) {
                logger.info("Downloading file...");
                Files.copy(inputStream, downloadedUpdatePath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Update downloaded successfully to: {}", downloadedUpdatePath);
                logger.info("Downloaded file size: {} bytes", Files.size(downloadedUpdatePath));
            }

        } catch (Exception e) {
            logger.error("Failed to download update from URL: {}", downloadUrl, e);
            throw new RuntimeException("Update download failed: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isUpdateDownloaded() {
        boolean exists = Files.exists(downloadedUpdatePath);
        logger.debug("Update file exists: {} at path: {}", exists, downloadedUpdatePath);
        return exists;
    }

    @Override
    public void applyUpdate() {
        if (!isUpdateDownloaded()) {
            throw new IllegalStateException("No update file available to apply");
        }

        logger.info("Preparing to apply update...");

        try {
            // Extract the ZIP file
            logger.info("Extracting update ZIP...");
            extractZip(downloadedUpdatePath, extractedUpdatePath);

            // Get the installation directory
            String currentExecutablePath = getCurrentExecutablePath();
            Path installDir = Paths.get(currentExecutablePath).getParent();
            logger.info("Installation directory: {}", installDir);

            // Create update script
            String updateScript = createUpdateScript(
                installDir.toString(),
                extractedUpdatePath.toString()
            );
            Path scriptPath = updateDirectory.resolve("update.bat");
            Files.write(scriptPath, updateScript.getBytes());

            logger.info("Update script created at: {}", scriptPath);
            logger.info("Application will restart to apply update...");

            // Start the update process in a separate thread after a delay
            // This allows the HTTP response to be sent before shutdown
            new Thread(() -> {
                try {
                    // Wait for the response to be sent
                    Thread.sleep(1000);

                    logger.info("Starting update script...");

                    // Execute update script with admin privileges
                    String elevateCommand = String.format(
                        "powershell -Command \"Start-Process cmd -ArgumentList '/c \"\"%s\"\"' -Verb RunAs\"",
                        scriptPath.toString()
                    );
                    ProcessBuilder pb = new ProcessBuilder("cmd", "/c", elevateCommand);
                    pb.start();

                    // Give the script time to start
                    Thread.sleep(1000);

                    logger.info("Initiating graceful shutdown...");
                    // Graceful shutdown - allows cleanup of resources
                    System.exit(0);

                } catch (Exception e) {
                    logger.error("Failed to start update script: {}", e.getMessage());
                }
            }, "update-trigger").start();

        } catch (Exception e) {
            logger.error("Failed to apply update: {}", e.getMessage());
            throw new RuntimeException("Update application failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getCurrentVersion() {
        return currentVersion;
    }

    private String getCurrentExecutablePath() {
        // Try to get the actual executable path from the process
        try {
            String command = ProcessHandle.current().info().command().orElse("");
            if (command.endsWith(".exe")) {
                logger.info("Detected executable path: {}", command);
                return command;
            }
        } catch (Exception e) {
            logger.warn("Failed to get process command: {}", e.getMessage());
        }

        // Try to get JAR path from classpath
        String jarPath = System.getProperty("java.class.path");
        if (jarPath.contains(".jar") && !jarPath.contains(";")) {
            logger.info("Detected JAR path: {}", jarPath);
            return jarPath;
        }

        // Last resort fallback
        String fallbackPath = System.getProperty("user.dir") + "\\segari-printer-middleware.exe";
        logger.warn("Using fallback path: {}", fallbackPath);
        return fallbackPath;
    }

    private void extractZip(Path zipFile, Path targetDirectory) throws IOException {
        logger.info("Extracting ZIP file: {} to {}", zipFile, targetDirectory);

        // Clean target directory if it exists
        if (Files.exists(targetDirectory)) {
            logger.info("Cleaning existing extracted directory...");
            Files.walk(targetDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        logger.warn("Failed to delete: {}", path);
                    }
                });
        }

        Files.createDirectories(targetDirectory);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = targetDirectory.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                    logger.debug("Created directory: {}", targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    logger.debug("Extracted file: {}", targetPath);
                }
                zis.closeEntry();
            }
        }

        logger.info("ZIP extraction completed successfully");
    }

    private String createUpdateScript(String installDir, String extractedDir) {
        // Get the current username to run the app as the original user (not admin)
        String username = System.getProperty("user.name");
        String exePath = installDir + "\\segari-printer-middleware.exe";

        return String.format(
            "@echo off\n" +
            "echo ========================================\n" +
            "echo Segari Printer Middleware Update\n" +
            "echo ========================================\n" +
            "echo.\n" +
            "echo Installation directory: %s\n" +
            "echo Update source: %s\n" +
            "echo User: %s\n" +
            "echo.\n" +
            "echo Waiting for application to close...\n" +
            "timeout /t 5 /nobreak\n" +
            "echo.\n" +
            "echo [1/3] Deleting old installation files...\n" +
            "cd /d \"%s\"\n" +
            "for /d %%%%d in (*) do (\n" +
            "    echo Deleting directory: %%%%d\n" +
            "    rd /s /q \"%%%%d\" 2>nul\n" +
            ")\n" +
            "for %%%%f in (*) do (\n" +
            "    if not \"%%%%f\"==\"update.bat\" (\n" +
            "        echo Deleting file: %%%%f\n" +
            "        del /f /q \"%%%%f\" 2>nul\n" +
            "    )\n" +
            ")\n" +
            "echo Old files deleted.\n" +
            "echo.\n" +
            "echo [2/3] Copying new files...\n" +
            "xcopy \"%s\\*\" \"%s\\\" /E /I /H /Y\n" +
            "if errorlevel 1 (\n" +
            "    echo ERROR: Failed to copy new files!\n" +
            "    pause\n" +
            "    exit /b 1\n" +
            ")\n" +
            "echo New files copied successfully.\n" +
            "echo.\n" +
            "echo [3/3] Starting application...\n" +
            "echo Starting as user: %s\n" +
            "rem Drop admin privileges and start as normal user\n" +
            "explorer.exe \"%s\"\n" +
            "echo.\n" +
            "echo ========================================\n" +
            "echo Update completed successfully!\n" +
            "echo ========================================\n" +
            "echo.\n" +
            "timeout /t 3 /nobreak\n",
            installDir, extractedDir, username,
            installDir,
            extractedDir, installDir,
            username, exePath
        );
    }
}