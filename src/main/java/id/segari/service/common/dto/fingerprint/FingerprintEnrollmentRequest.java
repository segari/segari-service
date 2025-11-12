package id.segari.service.common.dto.fingerprint;

import id.segari.service.db.enums.TemplateGroup;

public record FingerprintEnrollmentRequest(String employeeId, TemplateGroup templateGroup) {
}
