package id.segari.service.common.dto.fingerprint;

import id.segari.service.db.enums.TemplateVendor;

public record FingerprintMachine(long productId, String productName, TemplateVendor vendor) {}
