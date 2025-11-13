package id.segari.service.common.dto.fingerprint;

public record FingerprintStatusResponse(FingerprintMachineStatus status, FingerprintMachine fingerprintMachine, String computerGuid, Long connectedWarehouseId) {
}
