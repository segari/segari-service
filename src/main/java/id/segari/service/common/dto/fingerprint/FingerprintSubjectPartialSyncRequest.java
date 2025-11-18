package id.segari.service.common.dto.fingerprint;

public record FingerprintSubjectPartialSyncRequest(long warehouseId, long internalToolsUserId, String token) {
}
