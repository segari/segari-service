package id.segari.service.common.dto.external;

public record SegariResponse<T>(String message, String code, T data) {
}
