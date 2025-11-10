package id.segari.service.service;

import id.segari.service.common.dto.fingerprint.FingerprintEnrollmentResponse;
import id.segari.service.common.dto.fingerprint.FingerprintIdentificationResponse;
import id.segari.service.common.dto.fingerprint.FingerprintMachine;

public interface FingerprintService {
    FingerprintMachine getFingerprintMachine();
    void connect();
    void disconnect();
    FingerprintEnrollmentResponse initEnrollment(String employeeId);
    FingerprintIdentificationResponse initIdentification();
}
