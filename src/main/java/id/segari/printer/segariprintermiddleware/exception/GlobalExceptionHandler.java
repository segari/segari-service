package id.segari.printer.segariprintermiddleware.exception;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import id.segari.printer.segariprintermiddleware.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InternalBaseException.class)
    public ResponseEntity<ErrorResponse> handlePrinterException(
            InternalBaseException ex,
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String errorMessage = errors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));

        log.error("Validation failed: {}", errorMessage);
        return new ResponseEntity<>(new ErrorResponse(InternalResponseCode.VALIDATION_ERROR, errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.error("Constraint violation: {}", errorMessage);
        return new ResponseEntity<>(new ErrorResponse(InternalResponseCode.VALIDATION_ERROR, errorMessage), HttpStatus.BAD_REQUEST);
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