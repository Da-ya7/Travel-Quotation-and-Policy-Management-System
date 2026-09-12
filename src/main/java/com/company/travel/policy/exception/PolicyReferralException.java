package com.company.travel.policy.exception;

public class PolicyReferralException extends RuntimeException {

    private final String errorCode;

    public PolicyReferralException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}