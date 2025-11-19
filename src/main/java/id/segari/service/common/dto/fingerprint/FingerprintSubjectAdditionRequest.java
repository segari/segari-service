package id.segari.service.common.dto.fingerprint;

import jakarta.validation.constraints.NotBlank;

public record FingerprintSubjectAdditionRequest(@NotBlank String employeeId, boolean adhoc) {
}
