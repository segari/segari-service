package id.segari.service.common.dto.fingerprint;

import id.segari.service.db.enums.TemplateGroup;
import id.segari.service.db.enums.TemplateVendor;

public record FingerprintSubjectResponse(
        long id,
        long internalToolsUserId,
        TemplateGroup templateGroup,
        TemplateVendor templateVendor,
        byte[] template
) {
}
