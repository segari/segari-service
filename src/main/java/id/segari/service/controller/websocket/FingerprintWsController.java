package id.segari.service.controller.websocket;

import id.segari.service.common.dto.fingerprint.FingerprintEnrollmentRequest;
import id.segari.service.common.dto.fingerprint.FingerprintEnrollmentResponse;
import id.segari.service.common.dto.fingerprint.FingerprintIdentificationResponse;
import id.segari.service.service.FingerprintService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import static id.segari.service.common.dto.fingerprint.FingerprintTopicConstants.*;

@Controller
public class FingerprintWsController {

    private final FingerprintService fingerprintService;

    public FingerprintWsController(FingerprintService fingerprintService) {
        this.fingerprintService = fingerprintService;
    }

    /**
     * Handle fingerprint enrollment requests
     * Client sends to: /app/fingerprint/enroll
     * Response broadcast to: /topic/fingerprint/enroll
     */
    @MessageMapping(ENROLL)
    @SendTo(ENROLL_TOPIC)
    public FingerprintEnrollmentResponse handleFingerprintEnroll(FingerprintEnrollmentRequest request) {
        return fingerprintService.initEnrollment(request.employeeId(), request.templateGroup());
    }

    /**
     * Handle fingerprint identification requests
     * Client sends to: /app/fingerprint/identify
     * Response broadcast to: /topic/fingerprint/identify
     */
    @MessageMapping(IDENTIFY)
    @SendTo(IDENTIFY_TOPIC)
    public FingerprintIdentificationResponse handleFingerprintIdentify() {
        return fingerprintService.initIdentification();
    }
}
