package com.company.travel.auth.service;

import com.company.travel.auth.dto.LoginRequest;
import com.company.travel.auth.dto.LoginResponse;
import com.company.travel.auth.dto.RefreshTokenRequest;
import com.company.travel.auth.entity.RefreshToken;
import com.company.travel.auth.entity.User;
import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.entity.UserGroupMapping;
import com.company.travel.auth.exception.UserNotMappedToGroupException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

        private final AuthenticationManager authenticationManager;
        private final UserService userService;
        private final JwtService jwtService;
        private final UserGroupService userGroupService;
        private final RefreshTokenService refreshTokenService;

        public LoginService(
                        AuthenticationManager authenticationManager,
                        UserService userService,
                        JwtService jwtService,
                        UserGroupService userGroupService,
                        RefreshTokenService refreshTokenService) {

                this.authenticationManager = authenticationManager;
                this.userService = userService;
                this.jwtService = jwtService;
                this.userGroupService = userGroupService;
                this.refreshTokenService = refreshTokenService;
        }

        public LoginResponse login(LoginRequest request) {

                // 1. Find the user
                User user = userService.findByUsername(request.getUsername());

                if (user == null) {
                        throw new IllegalArgumentException("User not found");
                }

                // 2. Check whether the user is active
                if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                        throw new IllegalArgumentException("User is inactive");
                }

                // 3. Check whether the user has an active group mapping
                UserGroupMapping mapping = userGroupService.findActiveGroupMapping(user.getId());

                if (mapping == null) {
                        throw new UserNotMappedToGroupException();
                }

                // 4. Authenticate username and password
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getUsername(),
                                                request.getPassword()));

                // 5. Find the actual group
                UserGroup group = userGroupService.findGroupById(mapping.getUserGroupId());

                if (group == null) {
                        throw new IllegalArgumentException("User group not found");
                }

                // 6. Generate access token
                String accessToken = jwtService.generateAccessToken(
                                user.getId(),
                                user.getUsername(),
                                group.getId(),
                                group.getGroupCode());

                // 7. Generate refresh token
                String refreshToken = refreshTokenService.createRefreshToken(user.getId());

                // 8. Return both tokens
                return new LoginResponse(
                                accessToken,
                                refreshToken);
        }

        @Transactional
        public LoginResponse refresh(RefreshTokenRequest request) {

                // 1. Validate the refresh token
                RefreshToken oldRefreshToken = refreshTokenService.validateRefreshToken(
                                request.getRefreshToken());

                // 2. Find the user who owns this refresh token
                User user = userService.findById(oldRefreshToken.getUserId());

                if (user == null) {
                        throw new IllegalArgumentException("User not found");
                }

                // 3. Check whether the user is still active
                if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                        throw new IllegalArgumentException("User is inactive");
                }

                // 4. Check whether the user still has an active group mapping
                UserGroupMapping mapping = userGroupService.findActiveGroupMapping(user.getId());

                if (mapping == null) {
                        throw new UserNotMappedToGroupException();
                }

                // 5. Find the actual group
                UserGroup group = userGroupService.findGroupById(mapping.getUserGroupId());

                if (group == null) {
                        throw new IllegalArgumentException("User group not found");
                }

                // 6. Revoke the old refresh token
                refreshTokenService.revokeRefreshToken(oldRefreshToken);

                // 7. Generate a new access token
                String accessToken = jwtService.generateAccessToken(
                                user.getId(),
                                user.getUsername(),
                                group.getId(),
                                group.getGroupCode());

                // 8. Generate a new refresh token
                String newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

                // 9. Return the new tokens
                return new LoginResponse(
                                accessToken,
                                newRefreshToken);
        }
}