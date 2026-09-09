package com.company.travel.auth.controller;

import com.company.travel.auth.dto.AssignGroupAuthoritiesRequest;
import com.company.travel.auth.dto.CreateUserGroupRequest;
import com.company.travel.auth.dto.CreateUserRequest;
import com.company.travel.auth.dto.UserAuthorityUpdateRequest;
import com.company.travel.auth.dto.UserGroupMappingRequest;
import com.company.travel.auth.dto.UserGroupResponse;
import com.company.travel.auth.dto.UserResponse;
import com.company.travel.auth.entity.User;
import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.entity.UserGroupMapping;
import com.company.travel.auth.service.UserAuthorityService;
import com.company.travel.auth.service.UserGroupMappingService;
import com.company.travel.auth.service.UserGroupService;
import com.company.travel.auth.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Thin controller: no business logic here, only
// request -> service call -> response mapping.
// @PreAuthorize on each method matches the SRS authority matrix
// exactly (never hasRole('SYSTEM_ADMIN') — model is authority-based).
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

        private final UserGroupService userGroupService;
        private final UserService userService;
        private final UserGroupMappingService userGroupMappingService;
        private final UserAuthorityService userAuthorityService;

        public AdminController(
                        UserGroupService userGroupService,
                        UserService userService,
                        UserGroupMappingService userGroupMappingService,
                        UserAuthorityService userAuthorityService) {

                this.userGroupService = userGroupService;
                this.userService = userService;
                this.userGroupMappingService = userGroupMappingService;
                this.userAuthorityService = userAuthorityService;
        }

        // -----------------------------------------------------
        // User groups
        // -----------------------------------------------------

        @PostMapping("/user-groups")
        @PreAuthorize("hasAuthority('USER_GROUP_CREATE')")
        public ResponseEntity<UserGroupResponse> createGroup(
                        @Valid @RequestBody CreateUserGroupRequest request) {

                UserGroup group = userGroupService.createGroup(
                                request.getGroupCode(),
                                request.getGroupName(),
                                request.getStatus(),
                                request.getAuthorities());

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(toGroupResponse(group));
        }

        @GetMapping("/user-groups")
        @PreAuthorize("hasAuthority('USER_GROUP_CREATE')")
        public ResponseEntity<List<UserGroupResponse>> listGroups() {

                List<UserGroupResponse> response = userGroupService.findAllGroups()
                                .stream()
                                .map(this::toGroupResponse)
                                .toList();

                return ResponseEntity.ok(response);
        }

        @PostMapping("/user-groups/{id}/authorities")
        @PreAuthorize("hasAuthority('USER_GROUP_CREATE')")
        public ResponseEntity<UserGroupResponse> assignGroupAuthorities(
                        @PathVariable("id") Long groupId,
                        @Valid @RequestBody AssignGroupAuthoritiesRequest request) {

                UserGroup group = userGroupService.assignAuthorities(
                                groupId, request.getAuthorities());

                return ResponseEntity.ok(toGroupResponse(group));
        }

        // -----------------------------------------------------
        // Users
        // -----------------------------------------------------

        @PostMapping("/users")
        @PreAuthorize("hasAuthority('USER_CREATE')")
        public ResponseEntity<UserResponse> createUser(
                        @Valid @RequestBody CreateUserRequest request) {

                User user = userService.createUser(
                                request.getUsername(),
                                request.getEmail(),
                                request.getFullName(),
                                request.getPassword(),
                                request.getStatus());

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(toUserResponse(user));
        }

        // -----------------------------------------------------
        // User -> group mapping
        // -----------------------------------------------------

        @PostMapping("/users/{id}/group-mapping")
        @PreAuthorize("hasAuthority('USER_MAP_GROUP')")
        public ResponseEntity<Map<String, Object>> mapUserToGroup(
                        @PathVariable("id") Long userId,
                        @Valid @RequestBody UserGroupMappingRequest request) {

                UserGroupMapping mapping = userGroupMappingService.mapUserToGroup(
                                userId,
                                request.getUserGroupId(),
                                request.getEffectiveFrom());

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(Map.of(
                                                "userId", mapping.getUserId(),
                                                "userGroupId", mapping.getUserGroupId(),
                                                "effectiveFrom", mapping.getEffectiveFrom(),
                                                "effectiveTo",
                                                mapping.getEffectiveTo() == null ? "" : mapping.getEffectiveTo()));
        }

        // -----------------------------------------------------
        // User-level GRANT / REVOKE
        // -----------------------------------------------------

        @PutMapping("/users/{id}/authorities")
        @PreAuthorize("hasAuthority('USER_MAP_GROUP')")
        public ResponseEntity<Void> updateUserAuthorities(
                        @PathVariable("id") Long userId,
                        @Valid @RequestBody UserAuthorityUpdateRequest request) {

                userAuthorityService.updateGrantsAndRevokes(
                                userId, request.getGrants(), request.getRevokes());

                return ResponseEntity.noContent().build();
        }

        // -----------------------------------------------------
        // Mappers
        // -----------------------------------------------------

        private UserGroupResponse toGroupResponse(UserGroup group) {

                return new UserGroupResponse(
                                group.getId(),
                                group.getGroupCode(),
                                group.getGroupName(),
                                group.getStatus(),
                                userGroupService.getAuthorityCodesForGroup(group.getId()));
        }

        private UserResponse toUserResponse(User user) {

                return new UserResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getFullName(),
                                user.getStatus());
        }
}