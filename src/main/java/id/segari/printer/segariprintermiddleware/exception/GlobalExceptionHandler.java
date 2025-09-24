package id.segari.printer.segariprintermiddleware.exception;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import id.segari.printer.segariprintermiddleware.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PrinterException.class)
    public ResponseEntity<ErrorResponse> handlePrinterException(
            PrinterException ex,
            HttpServletRequest request) {

        log.error("PrinterException occurred: {} - Code: {}", ex.getMessage(), ex.getCode(), ex);
        return new ResponseEntity<>(new ErrorResponse(ex.getCode(), ex.getMessage()), ex.getHttpStatus());
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            BaseException ex,
            HttpServletRequest request) {

        log.error("BaseException occurred: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(new ErrorResponse(InternalResponseCode.BASIC, ex.getMessage()), ex.getHttpStatus());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        log.error("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return new ResponseEntity<>(new ErrorResponse(InternalResponseCode.NOT_FOUND, "Endpoint not found"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.error("IllegalArgumentException occurred: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(new ErrorResponse(InternalResponseCode.INVALID_ARGUMENT, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(new ErrorResponse(InternalResponseCode.INTERNAL_ERROR, ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}