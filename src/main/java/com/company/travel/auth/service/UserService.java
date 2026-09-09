package com.company.travel.auth.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.exception.DuplicateUserException;
import com.company.travel.auth.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElse(null);
    }

    /**
     * Creates a new user. We check username/email uniqueness
     * ourselves instead of relying only on the DB unique
     * constraint — a raw constraint violation surfaces as a
     * generic 500 unless caught, and we want a clean 409 with
     * the exact reason.
     *
     * The plaintext password is BCrypt-hashed before the entity
     * is saved and is never persisted or returned.
     */
    @Transactional
    public User createUser(
            String username,
            String email,
            String fullName,
            String plaintextPassword,
            String status) {

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUserException(
                    "USERNAME_ALREADY_EXISTS",
                    "Username already exists: " + username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateUserException(
                    "EMAIL_ALREADY_EXISTS",
                    "Email already exists: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(plaintextPassword));
        user.setStatus(status);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
}