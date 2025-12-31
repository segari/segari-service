package id.segari.service.common.dto.fingerprint;

import jakarta.validation.constraints.NotNull;

public record FingerprintSubjectSyncRequest(@NotNull Long warehouseId) {
}
