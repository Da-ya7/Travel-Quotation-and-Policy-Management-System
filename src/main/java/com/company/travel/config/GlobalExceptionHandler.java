package com.company.travel.config;

import com.company.travel.auth.exception.DuplicateUserException;
import com.company.travel.auth.exception.GroupAlreadyExistsException;
import com.company.travel.auth.exception.InactiveGroupException;
import com.company.travel.auth.exception.InvalidAuthorityException;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.exception.UserNotMappedToGroupException;
import com.company.travel.document.exception.InvalidDocumentsException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        // Phase 3 — unchanged
        @ExceptionHandler(UserNotMappedToGroupException.class)
        public ResponseEntity<Map<String, String>> handleUserNotMappedToGroup(
                        UserNotMappedToGroupException exception) {

                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(Map.of(
                                                "error", "USER_NOT_MAPPED_TO_GROUP"));
        }

        // ---- Phase 4 additions below ----

        @ExceptionHandler(GroupAlreadyExistsException.class)
        public ResponseEntity<Map<String, String>> handleGroupAlreadyExists(
                        GroupAlreadyExistsException exception) {

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(Map.of(
                                                "error", "GROUP_ALREADY_EXISTS"));
        }

        @ExceptionHandler(DuplicateUserException.class)
        public ResponseEntity<Map<String, String>> handleDuplicateUser(
                        DuplicateUserException exception) {

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(Map.of(
                                                "error", exception.getErrorCode()));
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleResourceNotFound(
                        ResourceNotFoundException exception) {

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(Map.of(
                                                "error", exception.getErrorCode()));
        }

        @ExceptionHandler(InvalidAuthorityException.class)
        public ResponseEntity<Map<String, Object>> handleInvalidAuthority(
                        InvalidAuthorityException exception) {

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("error", "INVALID_AUTHORITY");
                body.put("unknownAuthorities", exception.getUnknownAuthorities());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(body);
        }

        @ExceptionHandler(InactiveGroupException.class)
        public ResponseEntity<Map<String, String>> handleInactiveGroup(
                        InactiveGroupException exception) {

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "error", "GROUP_NOT_ACTIVE"));
        }

        // Note: this also now catches IllegalArgumentException thrown
        // by LoginService (e.g. "User not found"), which previously
        // fell through to a generic 500. That's an incidental fix,
        // not a behavior change to anything that was tested/working.
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, String>> handleIllegalArgument(
                        IllegalArgumentException exception) {

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "error", "INVALID_REQUEST",
                                                "message", exception.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidation(
                        MethodArgumentNotValidException exception) {

                Map<String, String> fieldErrors = new LinkedHashMap<>();

                exception.getBindingResult().getFieldErrors()
                                .forEach(fieldError -> fieldErrors.put(fieldError.getField(),
                                                fieldError.getDefaultMessage()));

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("error", "VALIDATION_FAILED");
                body.put("fields", fieldErrors);

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(body);
        }

        @ExceptionHandler(InvalidDocumentsException.class)
        public ResponseEntity<Map<String, Object>> handleInvalidDocuments(
                        InvalidDocumentsException exception) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("error", "INVALID_DOCUMENTS_UPLOADED");
                body.put("message", exception.getMessage());
                body.put("details", exception.getDetails());
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
        }
}