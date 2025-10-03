package id.segari.service.controller;

import id.segari.service.common.InternalResponseCode;
import id.segari.service.common.dto.update.VersionInfo;
import id.segari.service.common.response.SuccessResponse;
import id.segari.service.service.UpdateService;
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
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, updateService.checkForUpdates());
    }

    @PostMapping("/download")
    public SuccessResponse<Boolean> downloadUpdate(@RequestParam String downloadUrl) {
        updateService.downloadUpdate(downloadUrl);
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }

    @GetMapping("/status")
    public SuccessResponse<Boolean> getUpdateStatus() {
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, updateService.isUpdateDownloaded());
    }

    @PostMapping("/apply")
    public SuccessResponse<Boolean> applyUpdate() {
        updateService.applyUpdate();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }

    @GetMapping("/version")
    public SuccessResponse<String> getCurrentVersion() {
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, updateService.getCurrentVersion());
    }
}