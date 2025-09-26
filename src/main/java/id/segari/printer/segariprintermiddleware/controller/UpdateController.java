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