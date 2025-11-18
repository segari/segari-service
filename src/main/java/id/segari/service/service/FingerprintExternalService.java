package id.segari.service.service;

import id.segari.service.common.dto.fingerprint.FingerprintSubjectResponse;

import java.util.List;

public interface FingerprintExternalService {
    List<FingerprintSubjectResponse> getFingerprintSubject(long warehouseId, String token);
    List<FingerprintSubjectResponse> getFingerprintSubject(long warehouseId, long latestInternalToolsId, String token);
    List<FingerprintSubjectResponse> getFingerprintSubject(String employeeId, String token);
}
