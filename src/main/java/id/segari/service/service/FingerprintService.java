package id.segari.service.service;

import id.segari.service.common.dto.fingerprint.FingerprintEnrollmentResponse;
import id.segari.service.common.dto.fingerprint.FingerprintIdentificationResponse;
import id.segari.service.common.dto.fingerprint.FingerprintStatusResponse;
import id.segari.service.db.enums.TemplateGroup;

public interface FingerprintService {
    void connect(long warehouseId);
    void disconnect();
    FingerprintStatusResponse getFingerprintStatus();
    FingerprintEnrollmentResponse initEnrollment(String employeeId, TemplateGroup templateGroup);
    FingerprintIdentificationResponse initIdentification();
    void initNone();
    void sync(long warehouseId, String token);
    void sync(long warehouseId, long internalToolsUserId, String token);
    void add(String employeeId, boolean adhoc, String token);
}
