package id.segari.service.controller;

import id.segari.service.common.InternalResponseCode;
import id.segari.service.common.dto.fingerprint.FingerprintStatusResponse;
import id.segari.service.common.response.SuccessResponse;
import id.segari.service.service.FingerprintService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/fingerprint")
public class FingerprintController {

    private final FingerprintService fingerprintService;

    public FingerprintController(FingerprintService fingerprintService) {
        this.fingerprintService = fingerprintService;
    }

    @GetMapping("/status")
    public SuccessResponse<FingerprintStatusResponse> getFingerprintMachine() {
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, fingerprintService.getFingerprintStatus());
    }

    @PostMapping("/connect/{warehouseId}")
    public SuccessResponse<Boolean> connect(@PathVariable long warehouseId) {
        fingerprintService.connect(warehouseId);
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }

    @PostMapping("/disconnect")
    public SuccessResponse<Boolean> disconnect() {
        fingerprintService.disconnect();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }
}
