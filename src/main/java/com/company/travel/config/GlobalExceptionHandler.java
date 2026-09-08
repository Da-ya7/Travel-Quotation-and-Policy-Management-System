package com.company.travel.config;

import com.company.travel.auth.exception.UserNotMappedToGroupException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotMappedToGroupException.class)
    public ResponseEntity<Map<String, String>> handleUserNotMappedToGroup(
            UserNotMappedToGroupException exception) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "USER_NOT_MAPPED_TO_GROUP"));
    }
}