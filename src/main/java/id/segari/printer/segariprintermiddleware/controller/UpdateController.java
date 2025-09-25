package id.segari.printer.segariprintermiddleware.controller;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import id.segari.printer.segariprintermiddleware.common.dto.update.VersionInfo;
import id.segari.printer.segariprintermiddleware.common.response.SuccessResponse;
import id.segari.printer.segariprintermiddleware.service.UpdateService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/update")
public class UpdateController {
    private final UpdateService updateService;

    public UpdateController(UpdateService updateService) {
        this.updateService = updateService;
    }

    @GetMapping("/check")
    public SuccessResponse<VersionInfo> checkForUpdates() {
        VersionInfo versionInfo = updateService.checkForUpdates();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, versionInfo);
    }

    @PostMapping("/download")
    public SuccessResponse<String> downloadUpdate(@RequestParam String downloadUrl) {
        try {
            updateService.downloadUpdate(downloadUrl);
            return new SuccessResponse<>(InternalResponseCode.SUCCESS, "Update downloaded successfully");
        } catch (Exception e) {
            return new SuccessResponse<>(InternalResponseCode.SUCCESS, "Download failed: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public SuccessResponse<Boolean> getUpdateStatus() {
        boolean isDownloaded = updateService.isUpdateDownloaded();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, isDownloaded);
    }

    @PostMapping("/apply")
    public SuccessResponse<String> applyUpdate() {
        try {
            updateService.applyUpdate();
            return new SuccessResponse<>(InternalResponseCode.SUCCESS, "Update is being applied. Application will restart.");
        } catch (Exception e) {
            return new SuccessResponse<>(InternalResponseCode.SUCCESS, "Update failed: " + e.getMessage());
        }
    }

    @GetMapping("/version")
    public SuccessResponse<String> getCurrentVersion() {
        String version = updateService.getCurrentVersion();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, version);
    }
}