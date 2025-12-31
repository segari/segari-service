package id.segari.service.service;

import id.segari.service.common.dto.fingerprint.FingerprintSubjectResponse;

import java.util.List;

public interface FingerprintSubjectExternalService {
    List<FingerprintSubjectResponse> getFingerprintSubject(long warehouseId, String deviceId, String sessionId);
    List<FingerprintSubjectResponse> getFingerprintSubject(List<Long> internalToolsUserIds, String deviceId, String sessionId);
    List<FingerprintSubjectResponse> getFingerprintSubject(String employeeId, String deviceId, String sessionId);
}
