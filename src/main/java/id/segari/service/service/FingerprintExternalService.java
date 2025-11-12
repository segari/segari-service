package id.segari.service.service;

import id.segari.service.common.dto.fingerprint.FingerprintSubjectResponse;

import java.util.List;

public interface FingerprintExternalService {
    List<FingerprintSubjectResponse> getFingerprintSubject(long warehouseId);
    List<FingerprintSubjectResponse> getFingerprintSubject(long warehouseId, long latestInternalToolsId);
    List<FingerprintSubjectResponse> getFingerprintSubject(String employeeId);
}
