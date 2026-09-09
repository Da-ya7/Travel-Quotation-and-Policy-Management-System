package com.company.travel.auth.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.exception.DuplicateUserException;
import com.company.travel.auth.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void duplicateUsernameRejected() {

        when(userRepository.existsByUsername("uw.ravi")).thenReturn(true);

        DuplicateUserException exception = assertThrows(DuplicateUserException.class,
                () -> userService.createUser("uw.ravi", "ravi@company.com", "Ravi Kumar", "Str0ng!Passw0rd", "ACTIVE"));

        assertEquals("USERNAME_ALREADY_EXISTS", exception.getErrorCode());
    }

    @Test
    void duplicateEmailRejected() {

        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail("ravi@company.com")).thenReturn(true);

        DuplicateUserException exception = assertThrows(DuplicateUserException.class, () -> userService
                .createUser("uw.ravi2", "ravi@company.com", "Ravi Kumar", "Str0ng!Passw0rd", "ACTIVE"));

        assertEquals("EMAIL_ALREADY_EXISTS", exception.getErrorCode());
    }

    @Test
    void passwordIsHashedNotStoredPlaintext() {

        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("Str0ng!Passw0rd")).thenReturn("bcrypt-hash-value");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createUser("uw.ravi", "ravi@company.com", "Ravi Kumar", "Str0ng!Passw0rd", "ACTIVE");

        assertEquals("bcrypt-hash-value", result.getPasswordHash());
        assertNotEquals("Str0ng!Passw0rd", result.getPasswordHash());
    }
}