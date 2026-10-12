package com.travelapp.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler: any exception thrown by a Controller/Service is ultimately caught here
 * and converted into the { code, message, data } format; the frontend never has to deal with raw stack traces.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ApiResponse.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /** Thrown when validating constraint annotations (e.g. @Max) on @RequestParam / @PathVariable; only triggers if the Controller class is annotated with @Validated */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        return ApiResponse.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknownException(Exception ex) {
        // In production, consider integrating a system like Sentry here for error reporting; for local dev just log to console
        log.error("Uncaught exception", ex);
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getDefaultMessage());
    }
}
