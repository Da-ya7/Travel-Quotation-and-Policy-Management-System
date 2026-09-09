package com.company.travel.auth.controller;

import com.company.travel.auth.service.UserAuthorityService;
import com.company.travel.auth.service.CustomUserDetailsService;
import com.company.travel.auth.service.JwtService;
import com.company.travel.auth.entity.User;
import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.entity.UserGroupMapping;
import com.company.travel.auth.exception.DuplicateUserException;
import com.company.travel.auth.exception.GroupAlreadyExistsException;
import com.company.travel.auth.exception.InactiveGroupException;
import com.company.travel.auth.exception.InvalidAuthorityException;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.service.UserGroupMappingService;
import com.company.travel.auth.service.UserGroupService;
import com.company.travel.auth.service.UserService;
import com.company.travel.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

// Tests authorization and validation on the admin endpoints.
// Business logic is covered separately in the *ServiceTest classes.
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserGroupService userGroupService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserGroupMappingService userGroupMappingService;

    @MockitoBean
    private UserAuthorityService userAuthorityService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Default stubs — can be overridden in individual tests
        UserGroup group = new UserGroup();
        group.setId(1L);
        group.setGroupCode("TEST_GROUP");
        group.setGroupName("Test group");
        group.setStatus("ACTIVE");

        when(userGroupService.createGroup(
                anyString(), anyString(), anyString(), anyList()))
                .thenReturn(group);

        when(userGroupService.findAllGroups())
                .thenReturn(List.of(group));

        when(userGroupService.getAuthorityCodesForGroup(1L))
                .thenReturn(List.of("AUDIT_VIEW"));

        User user = new User();
        user.setId(1L);
        user.setUsername("newuser");
        user.setEmail("newuser@company.com");
        user.setFullName("New User");
        user.setStatus("ACTIVE");

        when(userService.createUser(
                anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(user);

        UserGroupMapping mapping = new UserGroupMapping();
        mapping.setId(1L);
        mapping.setUserId(1L);
        mapping.setUserGroupId(1L);
        mapping.setEffectiveFrom(LocalDate.of(2026, 9, 10));
        mapping.setEffectiveTo(null);

        when(userGroupMappingService.mapUserToGroup(any(), any(), any()))
                .thenReturn(mapping);

        doNothing().when(userAuthorityService)
                .updateGrantsAndRevokes(any(), anyList(), anyList());
    }

    // =====================================================================
    // Authorization boundary tests
    // =====================================================================

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {

        mockMvc.perform(post("/api/v1/admin/user-groups")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "groupCode", "TEST_GROUP",
                        "groupName", "Test group",
                        "status", "ACTIVE",
                        "authorities", List.of("AUDIT_VIEW")))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USER_GROUP_CREATE")
    void userGroupCreateAuthorityIsAllowedForGroupCreation() throws Exception {

        mockMvc.perform(post("/api/v1/admin/user-groups")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "groupCode", "TEST_GROUP",
                        "groupName", "Test group",
                        "status", "ACTIVE",
                        "authorities", List.of("AUDIT_VIEW")))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "QUOTATION_CREATE")
    void wrongAuthorityIsForbidden() throws Exception {

        mockMvc.perform(post("/api/v1/admin/user-groups")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "groupCode", "TEST_GROUP",
                        "groupName", "Test group",
                        "status", "ACTIVE",
                        "authorities", List.of("AUDIT_VIEW")))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER_CREATE")
    void userCreateAuthorityIsAllowedForUserCreation() throws Exception {

        mockMvc.perform(post("/api/v1/admin/users")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", "newuser",
                        "email", "newuser@company.com",
                        "fullName", "New User",
                        "password", "Str0ng!Passw0rd123",
                        "status", "ACTIVE"))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "USER_MAP_GROUP")
    void userMapGroupAuthorityIsAllowedForMapping() throws Exception {

        mockMvc.perform(post("/api/v1/admin/users/1/group-mapping")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "userGroupId", 1L,
                        "effectiveFrom", "2026-09-10"))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "USER_MAP_GROUP")
    void userMapGroupAuthorityIsAllowedForGrantRevoke() throws Exception {

        mockMvc.perform(put("/api/v1/admin/users/1/authorities")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "grants", List.of("QUOTATION_VIEW"),
                        "revokes", List.of()))))
                .andExpect(status().isNoContent());
    }

    // =====================================================================
    // Functional tests
    // =====================================================================

    @Test
    @WithMockUser(authorities = "USER_GROUP_CREATE")
    void createGroupSuccessfully() throws Exception {

        mockMvc.perform(post("/api/v1/admin/user-groups")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "groupCode", "TEST_GROUP",
                        "groupName", "Test group",
                        "status", "ACTIVE",
                        "authorities", List.of("AUDIT_VIEW")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.groupCode").value("TEST_GROUP"))
                .andExpect(jsonPath("$.groupName").value("Test group"));
    }

    @Test
    @WithMockUser(authorities = "USER_GROUP_CREATE")
    void listGroupsSuccessfully() throws Exception {

        mockMvc.perform(get("/api/v1/admin/user-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].groupCode").value("TEST_GROUP"));
    }

    @Test
    @WithMockUser(authorities = "USER_GROUP_CREATE")
    void createGroupWithDuplicateCodeReturnsConflict() throws Exception {

        when(userGroupService.createGroup(anyString(), anyString(), anyString(), anyList()))
                .thenThrow(new GroupAlreadyExistsException("TEST_GROUP"));

        mockMvc.perform(post("/api/v1/admin/user-groups")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "groupCode", "TEST_GROUP",
                        "groupName", "Test group",
                        "status", "ACTIVE",
                        "authorities", List.of("AUDIT_VIEW")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("GROUP_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(authorities = "USER_GROUP_CREATE")
    void createGroupWithInvalidAuthorityReturnsBadRequest() throws Exception {

        when(userGroupService.createGroup(anyString(), anyString(), anyString(), anyList()))
                .thenThrow(new InvalidAuthorityException(List.of("UNKNOWN_AUTHORITY")));

        mockMvc.perform(post("/api/v1/admin/user-groups")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "groupCode", "TEST_GROUP",
                        "groupName", "Test group",
                        "status", "ACTIVE",
                        "authorities", List.of("UNKNOWN_AUTHORITY")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_AUTHORITY"))
                .andExpect(jsonPath("$.unknownAuthorities[0]").value("UNKNOWN_AUTHORITY"));
    }

    @Test
    @WithMockUser(authorities = "USER_CREATE")
    void createUserSuccessfully() throws Exception {

        mockMvc.perform(post("/api/v1/admin/users")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", "newuser",
                        "email", "newuser@company.com",
                        "fullName", "New User",
                        "password", "Str0ng!Passw0rd123",
                        "status", "ACTIVE"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    @WithMockUser(authorities = "USER_CREATE")
    void createUserWithDuplicateUsernameReturnsConflict() throws Exception {

        when(userService.createUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new DuplicateUserException("USERNAME_ALREADY_EXISTS", "Username already exists"));

        mockMvc.perform(post("/api/v1/admin/users")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", "existing",
                        "email", "newuser@company.com",
                        "fullName", "New User",
                        "password", "Str0ng!Passw0rd123",
                        "status", "ACTIVE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(authorities = "USER_CREATE")
    void createUserWithDuplicateEmailReturnsConflict() throws Exception {

        when(userService.createUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new DuplicateUserException("EMAIL_ALREADY_EXISTS", "Email already exists"));

        mockMvc.perform(post("/api/v1/admin/users")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", "newuser",
                        "email", "existing@company.com",
                        "fullName", "New User",
                        "password", "Str0ng!Passw0rd123",
                        "status", "ACTIVE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(authorities = "USER_CREATE")
    void createUserWithInvalidPasswordReturnsBadRequest() throws Exception {

        mockMvc.perform(post("/api/v1/admin/users")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", "newuser",
                        "email", "newuser@company.com",
                        "fullName", "New User",
                        "password", "weak",
                        "status", "ACTIVE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "USER_MAP_GROUP")
    void mapUserToGroupSuccessfully() throws Exception {

        mockMvc.perform(post("/api/v1/admin/users/1/group-mapping")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "userGroupId", 1L,
                        "effectiveFrom", "2026-09-10"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.userGroupId").value(1));
    }

    @Test
    @WithMockUser(authorities = "USER_MAP_GROUP")
    void mapNonexistentUserReturnsNotFound() throws Exception {

        when(userGroupMappingService.mapUserToGroup(eq(999L), any(), any()))
                .thenThrow(new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

        mockMvc.perform(post("/api/v1/admin/users/999/group-mapping")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "userGroupId", 1L,
                        "effectiveFrom", "2026-09-10"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    @WithMockUser(authorities = "USER_MAP_GROUP")
    void mapToNonexistentGroupReturnsNotFound() throws Exception {

        when(userGroupMappingService.mapUserToGroup(any(), eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("GROUP_NOT_FOUND", "Group not found"));

        mockMvc.perform(post("/api/v1/admin/users/1/group-mapping")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "userGroupId", 999L,
                        "effectiveFrom", "2026-09-10"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("GROUP_NOT_FOUND"));
    }

    @Test
    @WithMockUser(authorities = "USER_MAP_GROUP")
    void mapToInactiveGroupReturnsBadRequest() throws Exception {

        when(userGroupMappingService.mapUserToGroup(any(), any(), any()))
                .thenThrow(new InactiveGroupException("INACTIVE_GROUP"));

        mockMvc.perform(post("/api/v1/admin/users/1/group-mapping")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "userGroupId", 1L,
                        "effectiveFrom", "2026-09-10"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("GROUP_NOT_ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = "USER_MAP_GROUP")
    void grantUserAuthoritySuccessfully() throws Exception {

        mockMvc.perform(put("/api/v1/admin/users/1/authorities")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "grants", List.of("QUOTATION_VIEW"),
                        "revokes", List.of()))))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "USER_MAP_GROUP")
    void revokeUserAuthoritySuccessfully() throws Exception {

        mockMvc.perform(put("/api/v1/admin/users/1/authorities")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "grants", List.of(),
                        "revokes", List.of("QUOTATION_VIEW")))))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "USER_MAP_GROUP")
    void grantAndRevokeInSameBadRequest() throws Exception {

        doThrow(new IllegalArgumentException("Authority cannot be both granted and revoked"))
                .when(userAuthorityService)
                .updateGrantsAndRevokes(any(), anyList(), anyList());

        mockMvc.perform(put("/api/v1/admin/users/1/authorities")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "grants", List.of("QUOTATION_VIEW"),
                        "revokes", List.of("QUOTATION_VIEW")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "USER_MAP_GROUP")
    void updateAuthorityForNonexistentUserReturnsNotFound() throws Exception {

        doThrow(new ResourceNotFoundException("USER_NOT_FOUND", "User not found"))
                .when(userAuthorityService)
                .updateGrantsAndRevokes(eq(999L), anyList(), anyList());

        mockMvc.perform(put("/api/v1/admin/users/999/authorities")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "grants", List.of("QUOTATION_VIEW"),
                        "revokes", List.of()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    @WithMockUser(authorities = "USER_GROUP_CREATE")
    void createGroupWithInvalidGroupCodeReturnsBadRequest() throws Exception {

        // Pattern requires uppercase snake_case
        mockMvc.perform(post("/api/v1/admin/user-groups")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "groupCode", "invalid-code", // lowercase with hyphen
                        "groupName", "Test group",
                        "status", "ACTIVE",
                        "authorities", List.of("AUDIT_VIEW")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "USER_GROUP_CREATE")
    void createGroupWithoutAuthoritiesReturnsBadRequest() throws Exception {

        mockMvc.perform(post("/api/v1/admin/user-groups")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "groupCode", "TEST_GROUP",
                        "groupName", "Test group",
                        "status", "ACTIVE",
                        "authorities", List.of())))) // Empty list
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }
}