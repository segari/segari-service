package id.segari.service.common.dto.fingerprint;

import id.segari.service.db.enums.TemplateGroup;
import id.segari.service.db.enums.TemplateVendor;

public record FingerprintEnrollmentResponse(FingerprintEnrollmentStatus status, int required, int scanned,
                                            TemplateVendor templateVendor, TemplateGroup templateGroup,
                                            String employeeId, byte[] template) {
}
