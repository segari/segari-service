package id.segari.service.common.dto.fingerprint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FingerprintSubjectPartialSyncRequest(@NotNull Long warehouseId, @NotNull Long internalToolsUserId) {
}
