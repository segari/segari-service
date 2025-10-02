package id.segari.printer.segariprintermiddleware.service.impl;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import id.segari.printer.segariprintermiddleware.common.dto.external.AppVersion;
import id.segari.printer.segariprintermiddleware.common.dto.external.SegariResponse;
import id.segari.printer.segariprintermiddleware.common.dto.update.VersionInfo;
import id.segari.printer.segariprintermiddleware.exception.InternalBaseException;
import id.segari.printer.segariprintermiddleware.service.UpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
            throw new InternalBaseException(InternalResponseCode.UPDATE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
        }
    }

    @Override
    public VersionInfo checkForUpdates() {
        if (!updateCheckEnabled || updateServerUrl.isEmpty()) {
            return new VersionInfo(currentVersion, currentVersion, false, "", "");
        }

        try {
            SegariResponse<AppVersion> response = restTemplate.exchange(
                updateServerUrl + "/v1/warehouse-printers/printer-middleware/versions",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<SegariResponse<AppVersion>>() {}
            ).getBody();

            if (response != null && response.data() != null) {
                return new VersionInfo(currentVersion, response.data().version(), isUpdateAvailable(currentVersion, response.data().version()), response.data().url(), response.message());
            }

            throw new InternalBaseException(InternalResponseCode.UPDATE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Invalid response from update server");

        } catch (InternalBaseException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalBaseException(InternalResponseCode.UPDATE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
        }
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
            throw new InternalBaseException(InternalResponseCode.UPDATE_ERROR, HttpStatus.BAD_REQUEST, "Error comparing versions: " + e.toString());
        }
    }

    @Override
    public void downloadUpdate(String downloadUrl) {
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            throw new InternalBaseException(InternalResponseCode.UPDATE_ERROR, HttpStatus.BAD_REQUEST, "Download URL cannot be empty");
        }

        try {
            // Ensure update directory exists
            if (!Files.exists(updateDirectory)) {
                Files.createDirectories(updateDirectory);
            }

            URL url = new URL(downloadUrl);

            try (InputStream inputStream = url.openStream()) {
                Files.copy(inputStream, downloadedUpdatePath, StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (Exception e) {
            throw new InternalBaseException(InternalResponseCode.UPDATE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
        }
    }

    @Override
    public boolean isUpdateDownloaded() {
        return Files.exists(downloadedUpdatePath);
    }

    @Override
    public void applyUpdate() {
        if (!isUpdateDownloaded()) {
            throw new InternalBaseException(InternalResponseCode.UPDATE_ERROR, HttpStatus.BAD_REQUEST, "No update file available to apply");
        }

        try {
            // Extract the ZIP file
            extractZip(downloadedUpdatePath, extractedUpdatePath);

            // Get the installation directory
            String currentExecutablePath = getCurrentExecutablePath();
            Path installDir = Paths.get(currentExecutablePath).getParent();

            // Create update script
            String updateScript = createUpdateScript(
                installDir.toString(),
                extractedUpdatePath.toString()
            );
            Path scriptPath = updateDirectory.resolve("update.bat");
            Files.write(scriptPath, updateScript.getBytes());

            // Start the update process in a separate thread after a delay
            // This allows the HTTP response to be sent before shutdown
            new Thread(() -> {
                try {
                    // Wait for the response to be sent
                    Thread.sleep(1000);

                    // Execute update script with admin privileges
                    String elevateCommand = String.format(
                        "powershell -Command \"Start-Process cmd -ArgumentList '/c \"\"%s\"\"' -Verb RunAs\"",
                        scriptPath.toString()
                    );
                    ProcessBuilder pb = new ProcessBuilder("cmd", "/c", elevateCommand);
                    pb.start();

                    // Give the script time to start
                    Thread.sleep(1000);

                    // Graceful shutdown - allows cleanup of resources
                    System.exit(0);

                } catch (Exception e) {
                    throw new InternalBaseException(InternalResponseCode.UPDATE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
                }
            }, "update-trigger").start();

        } catch (Exception e) {
            throw new InternalBaseException(InternalResponseCode.UPDATE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
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
                return command;
            }
        } catch (Exception e) {
            throw new InternalBaseException(InternalResponseCode.UPDATE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
        }

        // Try to get JAR path from classpath
        String jarPath = System.getProperty("java.class.path");
        if (jarPath.contains(".jar") && !jarPath.contains(";")) {
            return jarPath;
        }

        // Last resort fallback
        String fallbackPath = System.getProperty("user.dir") + "\\segari-printer-middleware.exe";
        return fallbackPath;
    }

    private void extractZip(Path zipFile, Path targetDirectory) throws IOException {
        // Clean target directory if it exists
        if (Files.exists(targetDirectory)) {
            Files.walk(targetDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new InternalBaseException(InternalResponseCode.UPDATE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
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
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private String createUpdateScript(String installDir, String extractedDir) {
        // Get the current username to run the app as the original user (not admin)
        String username = System.getProperty("user.name");
        String exePath = installDir + "\\segari-printer-middleware.exe";
        String updateZipPath = updateDirectory.resolve("update.zip").toString();
        String updateBatPath = updateDirectory.resolve("update.bat").toString();

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
            "echo [1/4] Deleting old installation files...\n" +
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
            "echo [2/4] Copying new files...\n" +
            "xcopy \"%s\\*\" \"%s\\\" /E /I /H /Y\n" +
            "if errorlevel 1 (\n" +
            "    echo ERROR: Failed to copy new files!\n" +
            "    pause\n" +
            "    exit /b 1\n" +
            ")\n" +
            "echo New files copied successfully.\n" +
            "echo.\n" +
            "echo [3/4] Cleaning up update files...\n" +
            "echo Deleting extracted directory: %s\n" +
            "rd /s /q \"%s\" 2>nul\n" +
            "echo Deleting update.zip: %s\n" +
            "del /f /q \"%s\" 2>nul\n" +
            "echo Update files cleaned up.\n" +
            "echo.\n" +
            "echo [4/4] Starting application...\n" +
            "echo Starting as user: %s\n" +
            "rem Drop admin privileges and start as normal user\n" +
            "explorer.exe \"%s\"\n" +
            "echo.\n" +
            "echo ========================================\n" +
            "echo Update completed successfully!\n" +
            "echo ========================================\n" +
            "echo.\n" +
            "echo Cleaning up update script...\n" +
            "timeout /t 2 /nobreak\n" +
            "del /f /q \"%s\" 2>nul\n",
            installDir, extractedDir, username,
            installDir,
            extractedDir, installDir,
            extractedDir, extractedDir,
            updateZipPath, updateZipPath,
            username, exePath,
            updateBatPath
        );
    }
}