package com.company.travel.auth.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.exception.UserNotMappedToGroupException;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;
    private final EffectiveAuthorityService effectiveAuthorityService;

    public CustomUserDetailsService(
            UserService userService,
            EffectiveAuthorityService effectiveAuthorityService) {

        this.userService = userService;
        this.effectiveAuthorityService = effectiveAuthorityService;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // 1. Find the user
        User user = userService.findByUsername(username);

        // User does not exist
        if (user == null) {
            throw new UsernameNotFoundException(
                    "User not found: " + username);
        }

        // 2. Check whether the user is active
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new UsernameNotFoundException(
                    "User is inactive: " + username);
        }

        // 3. Check whether the user has an active group mapping
        if (!effectiveAuthorityService.hasActiveGroupMapping(user.getId())) {
            throw new UserNotMappedToGroupException();
        }

        // 4. Calculate effective authorities
        Set<String> authorities = effectiveAuthorityService.getEffectiveAuthorities(
                user.getId());

        // 5. Build Spring Security UserDetails
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities.toArray(new String[0]))
                .build();
    }
}