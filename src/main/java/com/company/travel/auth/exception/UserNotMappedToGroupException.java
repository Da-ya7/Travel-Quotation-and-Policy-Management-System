package com.company.travel.auth.exception;

public class UserNotMappedToGroupException extends RuntimeException {

    public UserNotMappedToGroupException() {
        super("USER_NOT_MAPPED_TO_GROUP");
    }
}