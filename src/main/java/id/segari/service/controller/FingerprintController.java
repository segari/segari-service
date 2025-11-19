package id.segari.service.controller;

import id.segari.service.common.InternalResponseCode;
import id.segari.service.common.dto.fingerprint.*;
import id.segari.service.common.response.SuccessResponse;
import id.segari.service.service.FingerprintService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/fingerprint")
@Validated
public class FingerprintController {

    private final FingerprintService fingerprintService;

    public FingerprintController(FingerprintService fingerprintService) {
        this.fingerprintService = fingerprintService;
    }

    @GetMapping("/status")
    public SuccessResponse<FingerprintStatusResponse> getFingerprintMachine() {
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, fingerprintService.getFingerprintStatus());
    }

    @PostMapping("/connect")
    public SuccessResponse<Boolean> connect(@RequestBody @Valid FingerprintConnectRequest request) {
        fingerprintService.connect(request.warehouseId(), request.sessionId());
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }

    @PostMapping("/disconnect")
    public SuccessResponse<Boolean> disconnect() {
        fingerprintService.disconnect();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }

    @PostMapping("/subject/sync")
    public SuccessResponse<Boolean> sync(@RequestBody @Valid FingerprintSubjectSyncRequest request) {
        fingerprintService.sync(request.warehouseId());
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }

    @PostMapping("/subject/add")
    public SuccessResponse<Boolean> add(@RequestBody @Valid FingerprintSubjectAdditionRequest request) {
        fingerprintService.add(request.employeeId(), request.adhoc());
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }
}
