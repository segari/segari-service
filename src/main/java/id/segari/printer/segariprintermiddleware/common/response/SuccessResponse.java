package id.segari.printer.segariprintermiddleware.common.response;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;

public record SuccessResponse<T>(InternalResponseCode code, T data) {
}
