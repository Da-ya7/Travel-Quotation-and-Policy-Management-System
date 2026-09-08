package com.company.travel.auth.controller;

import com.company.travel.auth.dto.LoginRequest;
import com.company.travel.auth.dto.LoginResponse;
import com.company.travel.auth.dto.MeResponse;
import com.company.travel.auth.dto.RefreshTokenRequest;
import com.company.travel.auth.entity.User;
import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.entity.UserGroupMapping;
import com.company.travel.auth.service.EffectiveAuthorityService;
import com.company.travel.auth.service.LoginService;
import com.company.travel.auth.service.UserGroupService;
import com.company.travel.auth.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginService loginService;
    private final UserService userService;
    private final UserGroupService userGroupService;
    private final EffectiveAuthorityService effectiveAuthorityService;

    public AuthController(
            LoginService loginService,
            UserService userService,
            UserGroupService userGroupService,
            EffectiveAuthorityService effectiveAuthorityService) {

        this.loginService = loginService;
        this.userService = userService;
        this.userGroupService = userGroupService;
        this.effectiveAuthorityService = effectiveAuthorityService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                loginService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(
                loginService.refresh(request));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(
            Authentication authentication) {

        String username = authentication.getName();

        User user = userService.findByUsername(username);

        UserGroupMapping mapping = userGroupService.findActiveGroupMapping(user.getId());

        UserGroup group = userGroupService.findGroupById(
                mapping.getUserGroupId());

        Set<String> authorities = effectiveAuthorityService.getEffectiveAuthorities(
                user.getId());

        MeResponse response = new MeResponse(
                user.getId(),
                user.getUsername(),
                group.getId(),
                group.getGroupCode(),
                authorities);

        return ResponseEntity.ok(response);
    }
}