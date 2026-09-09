package com.company.travel.auth.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.entity.UserGroupMapping;
import com.company.travel.auth.exception.InactiveGroupException;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.repository.UserGroupMappingRepository;
import com.company.travel.auth.repository.UserGroupRepository;
import com.company.travel.auth.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserGroupMappingServiceTest {

    private UserRepository userRepository;
    private UserGroupRepository userGroupRepository;
    private UserGroupMappingRepository mappingRepository;
    private UserGroupMappingService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userGroupRepository = mock(UserGroupRepository.class);
        mappingRepository = mock(UserGroupMappingRepository.class);

        service = new UserGroupMappingService(
                userRepository,
                userGroupRepository,
                mappingRepository);
    }

    @Test
    void successfullyMapUserToActiveGroup() {

        User user = new User();
        user.setId(1L);

        UserGroup group = new UserGroup();
        group.setId(10L);
        group.setStatus("ACTIVE");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(mappingRepository.findOpenMapping(1L)).thenReturn(Optional.empty());

        UserGroupMapping saved = new UserGroupMapping();
        saved.setId(100L);
        saved.setUserId(1L);
        saved.setUserGroupId(10L);
        saved.setEffectiveFrom(LocalDate.of(2026, 9, 10));
        saved.setEffectiveTo(null);

        when(mappingRepository.save(any(UserGroupMapping.class))).thenReturn(saved);

        UserGroupMapping result = service.mapUserToGroup(
                1L, 10L, LocalDate.of(2026, 9, 10));

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(10L, result.getUserGroupId());
        assertEquals(LocalDate.of(2026, 9, 10), result.getEffectiveFrom());
        assertNull(result.getEffectiveTo());
    }

    @Test
    void mappingNonexistentUserFails() {

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.mapUserToGroup(
                        999L, 10L, LocalDate.of(2026, 9, 10)));

        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void mappingNonexistentGroupFails() {

        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userGroupRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.mapUserToGroup(
                        1L, 999L, LocalDate.of(2026, 9, 10)));

        assertEquals("GROUP_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void mappingToInactiveGroupIsRejected() {

        User user = new User();
        user.setId(1L);

        UserGroup inactiveGroup = new UserGroup();
        inactiveGroup.setId(10L);
        inactiveGroup.setStatus("INACTIVE");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userGroupRepository.findById(10L)).thenReturn(Optional.of(inactiveGroup));

        assertThrows(
                InactiveGroupException.class,
                () -> service.mapUserToGroup(
                        1L, 10L, LocalDate.of(2026, 9, 10)));
    }

    @Test
    void previousOpenMappingIsClosedWhenNewMappingCreated() {

        User user = new User();
        user.setId(1L);

        UserGroup group = new UserGroup();
        group.setId(10L);
        group.setStatus("ACTIVE");

        // Existing open mapping
        UserGroupMapping previousMapping = new UserGroupMapping();
        previousMapping.setId(50L);
        previousMapping.setUserId(1L);
        previousMapping.setUserGroupId(10L);
        previousMapping.setEffectiveFrom(LocalDate.of(2026, 9, 01));
        previousMapping.setEffectiveTo(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(mappingRepository.findOpenMapping(1L)).thenReturn(Optional.of(previousMapping));

        UserGroupMapping newMapping = new UserGroupMapping();
        newMapping.setId(100L);
        newMapping.setUserId(1L);
        newMapping.setUserGroupId(10L);
        newMapping.setEffectiveFrom(LocalDate.of(2026, 9, 10));
        newMapping.setEffectiveTo(null);

        when(mappingRepository.save(any(UserGroupMapping.class))).thenReturn(newMapping);

        UserGroupMapping result = service.mapUserToGroup(
                1L, 10L, LocalDate.of(2026, 9, 10));

        // Verify the previous mapping was closed (effectiveTo set to day before new
        // mapping)
        assertEquals(LocalDate.of(2026, 9, 9), previousMapping.getEffectiveTo());

        // Verify previous mapping was saved
        verify(mappingRepository).save(previousMapping);

        // Verify new mapping was created
        assertNotNull(result);
        assertEquals(LocalDate.of(2026, 9, 10), result.getEffectiveFrom());
        assertNull(result.getEffectiveTo());
    }

    @Test
    void dateLogicPreventsOverlapWhenClosingPreviousMapping() {

        // This verifies the date logic matches the effective mapping query:
        // findActiveMapping uses: effectiveFrom <= today AND (effectiveTo IS NULL OR
        // effectiveTo >= today)
        //
        // Old mapping: effective_from = 2026-09-01, effective_to = null (open)
        // New mapping: effective_from = 2026-09-10
        //
        // After closing: old mapping effective_to = 2026-09-09
        //
        // On 2026-09-09:
        // - Old: 09-01 <= 09-09 (yes) AND (null OR 09-09 >= 09-09) = YES
        // - New: 09-10 <= 09-09 (no) = NO
        // So no overlap on 09-09.
        //
        // On 2026-09-10:
        // - Old: 09-01 <= 09-10 (yes) AND (null OR 09-09 >= 09-10) = NO
        // - New: 09-10 <= 09-10 (yes) AND (null OR null >= 09-10) = YES
        // So no overlap on 09-10.

        User user = new User();
        user.setId(1L);

        UserGroup group = new UserGroup();
        group.setId(10L);
        group.setStatus("ACTIVE");

        UserGroupMapping previousMapping = new UserGroupMapping();
        previousMapping.setId(50L);
        previousMapping.setUserId(1L);
        previousMapping.setUserGroupId(10L);
        previousMapping.setEffectiveFrom(LocalDate.of(2026, 9, 1));
        previousMapping.setEffectiveTo(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(mappingRepository.findOpenMapping(1L)).thenReturn(Optional.of(previousMapping));

        UserGroupMapping newMapping = new UserGroupMapping();
        newMapping.setId(100L);
        newMapping.setUserId(1L);
        newMapping.setUserGroupId(10L);
        newMapping.setEffectiveFrom(LocalDate.of(2026, 9, 10));
        newMapping.setEffectiveTo(null);

        when(mappingRepository.save(any(UserGroupMapping.class))).thenReturn(newMapping);

        service.mapUserToGroup(1L, 10L, LocalDate.of(2026, 9, 10));

        // Verify the close date is one day before the new mapping's effectiveFrom
        assertEquals(LocalDate.of(2026, 9, 9), previousMapping.getEffectiveTo());
    }

    @Test
    void effectiveFromCannotBeBeforeCurrentMappingEffectiveFrom() {

        User user = new User();
        user.setId(1L);

        UserGroup group = new UserGroup();
        group.setId(10L);
        group.setStatus("ACTIVE");

        UserGroupMapping previousMapping = new UserGroupMapping();
        previousMapping.setId(50L);
        previousMapping.setUserId(1L);
        previousMapping.setUserGroupId(10L);
        previousMapping.setEffectiveFrom(LocalDate.of(2026, 9, 10));
        previousMapping.setEffectiveTo(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userGroupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(mappingRepository.findOpenMapping(1L)).thenReturn(Optional.of(previousMapping));

        // Try to create new mapping that ends before current one starts
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.mapUserToGroup(
                        1L, 10L, LocalDate.of(2026, 9, 9)));

        assertEquals(
                "effectiveFrom must be after the current mapping's effectiveFrom (2026-09-10)",
                exception.getMessage());
    }
}
