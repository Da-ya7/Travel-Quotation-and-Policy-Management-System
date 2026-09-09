package com.company.travel.auth.service;

import com.company.travel.auth.entity.Authority;
import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.exception.GroupAlreadyExistsException;
import com.company.travel.auth.exception.InvalidAuthorityException;
import com.company.travel.auth.repository.AuthorityRepository;
import com.company.travel.auth.repository.UserGroupAuthorityRepository;
import com.company.travel.auth.repository.UserGroupMappingRepository;
import com.company.travel.auth.repository.UserGroupRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserGroupServiceTest {

    private UserGroupRepository userGroupRepository;
    private AuthorityRepository authorityRepository;
    private UserGroupAuthorityRepository userGroupAuthorityRepository;
    private UserGroupService service;

    @BeforeEach
    void setUp() {
        userGroupRepository = mock(UserGroupRepository.class);
        authorityRepository = mock(AuthorityRepository.class);
        userGroupAuthorityRepository = mock(UserGroupAuthorityRepository.class);
        UserGroupMappingRepository mappingRepository = mock(UserGroupMappingRepository.class);

        service = new UserGroupService(
                mappingRepository,
                userGroupRepository,
                authorityRepository,
                userGroupAuthorityRepository);
    }

    @Test
    void duplicateGroupCodeReturnsGroupAlreadyExists() {

        when(userGroupRepository.existsByGroupCode("UNDERWRITING_USER")).thenReturn(true);

        assertThrows(GroupAlreadyExistsException.class,
                () -> service.createGroup("UNDERWRITING_USER", "Underwriting", "ACTIVE", List.of("AUDIT_VIEW")));
    }

    @Test
    void unknownAuthorityCodeIsRejected() {

        when(userGroupRepository.existsByGroupCode(any())).thenReturn(false);
        when(authorityRepository.findByAuthorityCodeIn(anyList())).thenReturn(List.of());

        InvalidAuthorityException exception = assertThrows(InvalidAuthorityException.class,
                () -> service.createGroup("NEW_GROUP", "New group", "ACTIVE", List.of("NOT_REAL_AUTHORITY")));

        assertEquals(List.of("NOT_REAL_AUTHORITY"), exception.getUnknownAuthorities());
    }

    @Test
    void validGroupCreationSaves() {

        Authority auditView = new Authority();
        auditView.setId(1L);
        auditView.setAuthorityCode("AUDIT_VIEW");

        when(userGroupRepository.existsByGroupCode(any())).thenReturn(false);
        when(authorityRepository.findByAuthorityCodeIn(anyList())).thenReturn(List.of(auditView));

        UserGroup saved = new UserGroup();
        saved.setId(10L);
        when(userGroupRepository.save(any(UserGroup.class))).thenReturn(saved);

        UserGroup result = service.createGroup("NEW_GROUP", "New group", "ACTIVE", List.of("AUDIT_VIEW"));

        assertEquals(10L, result.getId());
    }
}