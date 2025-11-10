package id.segari.service.common.dto.fingerprint;

public record FingerprintEnrollmentResponse(FingerprintEnrollmentStatus status, int required, int scanned,
                                            String employeeId, byte[] template) {
}
