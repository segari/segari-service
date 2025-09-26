package id.segari.printer.segariprintermiddleware.common.dto.external;

public record SegariResponse<T>(String message, String code, T data) {
}
