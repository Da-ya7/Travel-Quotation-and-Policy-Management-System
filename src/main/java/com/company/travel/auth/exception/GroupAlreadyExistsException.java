package com.company.travel.auth.exception;

public class GroupAlreadyExistsException extends RuntimeException {

    public GroupAlreadyExistsException(String groupCode) {
        super("User group already exists: " + groupCode);
    }
}