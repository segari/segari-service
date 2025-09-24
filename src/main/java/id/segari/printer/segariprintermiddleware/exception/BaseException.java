package id.segari.printer.segariprintermiddleware.exception;

import org.springframework.http.HttpStatus;

public class BaseException extends RuntimeException {

    private final HttpStatus httpStatus;

    public BaseException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public BaseException(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}