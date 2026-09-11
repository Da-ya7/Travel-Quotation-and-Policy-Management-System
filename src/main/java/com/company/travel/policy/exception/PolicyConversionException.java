package com.company.travel.policy.exception;

public class PolicyConversionException extends RuntimeException {

    private final String errorCode;

    public PolicyConversionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}