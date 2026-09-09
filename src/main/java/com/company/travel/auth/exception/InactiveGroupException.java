package com.company.travel.auth.exception;

public class InactiveGroupException extends RuntimeException {

    public InactiveGroupException(String groupCode) {
        super("User group is not ACTIVE: " + groupCode);
    }
}