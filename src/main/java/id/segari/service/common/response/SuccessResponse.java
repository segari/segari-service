package id.segari.service.common.response;

import id.segari.service.common.InternalResponseCode;

public record SuccessResponse<T>(InternalResponseCode code, T data) {
}
