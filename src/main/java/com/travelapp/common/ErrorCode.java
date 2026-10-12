package com.travelapp.common;

import lombok.Getter;

/**
 * Error code definitions, kept in sync with the error code table in section 1.3 of the API docs.
 * Please update the API docs whenever a new error code is added.
 */
@Getter
public enum ErrorCode {

    PARAM_INVALID(40001, "Parameter validation failed"),
    UNAUTHORIZED(40100, "Not logged in or token has expired"),
    FORBIDDEN(40300, "You do not have permission to access this resource"),
    RESOURCE_NOT_FOUND(40400, "Resource not found"),
    CONFLICT(40900, "Resource conflict"),
    TOO_MANY_REQUESTS(42900, "Too many requests, please try again later"),
    INTERNAL_ERROR(50000, "Internal server error"),
    THIRD_PARTY_UNAVAILABLE(50300, "A dependent third-party service is temporarily unavailable");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
