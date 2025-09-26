package id.segari.printer.segariprintermiddleware.exception;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import org.springframework.http.HttpStatus;

public class InternalBaseException extends BaseException {
    private final InternalResponseCode code;

    public InternalBaseException(InternalResponseCode code, HttpStatus httpStatus, String message) {
        super(httpStatus, message);
        this.code = code;
    }

    public InternalResponseCode getCode() {
        return code;
    }
}