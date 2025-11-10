package id.segari.service.controller;

import id.segari.service.common.InternalResponseCode;
import id.segari.service.common.dto.fingerprint.FingerprintMachine;
import id.segari.service.common.response.SuccessResponse;
import id.segari.service.service.FingerprintService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/fingerprint")
public class FingerprintController {

    private final FingerprintService fingerprintService;

    public FingerprintController(FingerprintService fingerprintService) {
        this.fingerprintService = fingerprintService;
    }

    @GetMapping("/plugged")
    public SuccessResponse<FingerprintMachine> getFingerprintMachine() {
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, fingerprintService.getFingerprintMachine());
    }

    @PostMapping("/connect")
    public SuccessResponse<Boolean> connect() {
        fingerprintService.connect();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }

    @PostMapping("/disconnect")
    public SuccessResponse<Boolean> disconnect() {
        fingerprintService.disconnect();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }
}
