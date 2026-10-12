package com.travelapp.common;

import lombok.Getter;

/**
 * Business exception. Thrown by the Service layer when validation fails, carrying an error code
 * defined in the API docs; caught centrally by GlobalExceptionHandler and converted into an
 * ApiResponse returned to the frontend.
 *
 * Example: throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "This travel note does not exist");
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
