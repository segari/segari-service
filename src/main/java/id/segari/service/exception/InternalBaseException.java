package id.segari.service.exception;

import id.segari.service.common.InternalResponseCode;
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