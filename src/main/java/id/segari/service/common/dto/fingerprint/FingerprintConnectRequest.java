package id.segari.service.common.dto.fingerprint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FingerprintConnectRequest(@NotNull Long warehouseId, @NotBlank String sessionId) {
}
