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

    public UpdateServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.updateDirectory = Paths.get(System.getProperty("user.home"), ".segari-printer", "updates");
        this.downloadedUpdatePath = updateDirectory.resolve("segari-printer-middleware-new.exe");

        try {
            Files.createDirectories(updateDirectory);
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

        try {
            URL url = new URL(downloadUrl);

            try (InputStream inputStream = url.openStream()) {
                Files.copy(inputStream, downloadedUpdatePath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Update downloaded successfully to: {}", downloadedUpdatePath);
            }

        } catch (IOException e) {
            logger.error("Failed to download update: {}", e.getMessage());
            throw new RuntimeException("Update download failed: " + e.getMessage(), e);
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
            String currentExecutablePath = getCurrentExecutablePath();
            String backupPath = currentExecutablePath + ".backup";

            // Create update script
            String updateScript = createUpdateScript(currentExecutablePath, downloadedUpdatePath.toString(), backupPath);
            Path scriptPath = updateDirectory.resolve("update.bat");
            Files.write(scriptPath, updateScript.getBytes());

            logger.info("Update script created at: {}", scriptPath);
            logger.info("Application will restart to apply update...");

            // Execute update script and exit current application
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", scriptPath.toString());
            pb.start();

            // Give the script time to start, then exit
            Thread.sleep(2000);
            System.exit(0);

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
        // Try to get the current executable path
        String jarPath = System.getProperty("java.class.path");

        if (jarPath.contains(".jar")) {
            return jarPath;
        }

        // Fallback for when running as executable
        return System.getProperty("user.dir") + "\\segari-printer-middleware.exe";
    }

    private String createUpdateScript(String currentExePath, String newExePath, String backupPath) {
        return String.format(
            "@echo off\n" +
            "echo Starting update process...\n" +
            "timeout /t 3 /nobreak >nul\n" +
            "echo Creating backup of current version...\n" +
            "copy \"%s\" \"%s\" >nul\n" +
            "echo Replacing with new version...\n" +
            "copy \"%s\" \"%s\" >nul\n" +
            "echo Cleaning up...\n" +
            "del \"%s\" >nul\n" +
            "echo Update completed. Starting application...\n" +
            "start \"\" \"%s\"\n" +
            "del \"%%~f0\" >nul\n",
            currentExePath, backupPath,
            newExePath, currentExePath,
            newExePath,
            currentExePath
        );
    }
}