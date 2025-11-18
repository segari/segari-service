package id.segari.service.common.dto.fingerprint;

public record FingerprintSubjectAdditionRequest(String employeeId, boolean adhoc, String token) {
}
