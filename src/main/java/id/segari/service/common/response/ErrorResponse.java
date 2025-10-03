package id.segari.service.common.response;

import id.segari.service.common.InternalResponseCode;

public record ErrorResponse(InternalResponseCode code, String message) {}
