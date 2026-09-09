package com.company.travel.auth.exception;

// Generic 404 carrier — errorCode lets one handler in
// GlobalExceptionHandler serve USER_NOT_FOUND, GROUP_NOT_FOUND, etc.
public class ResourceNotFoundException extends RuntimeException {

    private final String errorCode;

    public ResourceNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}