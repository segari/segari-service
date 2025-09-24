package id.segari.printer.segariprintermiddleware.common.response;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;

public record ErrorResponse(InternalResponseCode code, String message) {}
