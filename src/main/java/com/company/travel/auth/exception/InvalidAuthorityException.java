package com.company.travel.auth.exception;

import java.util.List;

public class InvalidAuthorityException extends RuntimeException {

    private final List<String> unknownAuthorities;

    public InvalidAuthorityException(List<String> unknownAuthorities) {
        super("Unknown authority code(s): " + unknownAuthorities);
        this.unknownAuthorities = unknownAuthorities;
    }

    public List<String> getUnknownAuthorities() {
        return unknownAuthorities;
    }
}