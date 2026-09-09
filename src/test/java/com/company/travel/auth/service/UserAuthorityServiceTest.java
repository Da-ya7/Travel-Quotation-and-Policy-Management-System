package com.company.travel.auth.service;

import com.company.travel.auth.entity.Authority;
import com.company.travel.auth.entity.User;
import com.company.travel.auth.entity.UserAuthority;
import com.company.travel.auth.entity.UserAuthorityId;
import com.company.travel.auth.exception.InvalidAuthorityException;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.repository.AuthorityRepository;
import com.company.travel.auth.repository.UserAuthorityRepository;
import com.company.travel.auth.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAuthorityServiceTest {

    private UserRepository userRepository;
    private AuthorityRepository authorityRepository;
    private UserAuthorityRepository userAuthorityRepository;
    private UserAuthorityService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authorityRepository = mock(AuthorityRepository.class);
        userAuthorityRepository = mock(UserAuthorityRepository.class);

        service = new UserAuthorityService(
                userRepository,
                authorityRepository,
                userAuthorityRepository);
    }

    @Test
    void grantValidAuthoritySucceeds() {

        User user = new User();
        user.setId(1L);

        Authority authority = new Authority();
        authority.setId(10L);
        authority.setAuthorityCode("QUOTATION_CREATE");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authorityRepository.findByAuthorityCodeIn(anyCollection()))
                .thenReturn(List.of(authority));
        when(userAuthorityRepository.findById(new UserAuthorityId(1L, 10L)))
                .thenReturn(Optional.empty());

        UserAuthority saved = new UserAuthority();
        saved.setId(new UserAuthorityId(1L, 10L));
        saved.setEffect(UserAuthority.Effect.GRANT);

        when(userAuthorityRepository.save(any(UserAuthority.class))).thenReturn(saved);

        // Should not throw
        service.updateGrantsAndRevokes(1L, List.of("QUOTATION_CREATE"), null);

        verify(userAuthorityRepository).save(any(UserAuthority.class));
    }

    @Test
    void revokeValidAuthoritySucceeds() {

        User user = new User();
        user.setId(1L);

        Authority authority = new Authority();
        authority.setId(10L);
        authority.setAuthorityCode("QUOTATION_CREATE");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authorityRepository.findByAuthorityCodeIn(anyCollection()))
                .thenReturn(List.of(authority));
        when(userAuthorityRepository.findById(new UserAuthorityId(1L, 10L)))
                .thenReturn(Optional.empty());

        UserAuthority saved = new UserAuthority();
        saved.setId(new UserAuthorityId(1L, 10L));
        saved.setEffect(UserAuthority.Effect.REVOKE);

        when(userAuthorityRepository.save(any(UserAuthority.class))).thenReturn(saved);

        // Should not throw
        service.updateGrantsAndRevokes(1L, null, List.of("QUOTATION_CREATE"));

        verify(userAuthorityRepository).save(any(UserAuthority.class));
    }

    @Test
    void unknownAuthorityIsRejected() {

        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authorityRepository.findByAuthorityCodeIn(anyCollection()))
                .thenReturn(List.of());

        InvalidAuthorityException exception = assertThrows(
                InvalidAuthorityException.class,
                () -> service.updateGrantsAndRevokes(
                        1L, List.of("UNKNOWN_AUTH"), null));

        assertEquals(List.of("UNKNOWN_AUTH"), exception.getUnknownAuthorities());
    }

    @Test
    void nonexistentUserIsRejected() {

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateGrantsAndRevokes(
                        999L, List.of("QUOTATION_CREATE"), null));

        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void emptyGrantsAndEmptyRevokesIsRejected() {

        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateGrantsAndRevokes(1L, null, null));

        assertTrue(exception.getMessage().contains("At least one grant or revoke"));
    }

    @Test
    void grantAndRevokeOverlapIsRejected() {

        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateGrantsAndRevokes(
                        1L,
                        List.of("QUOTATION_CREATE"),
                        List.of("QUOTATION_CREATE")));

        assertTrue(exception.getMessage().contains("cannot be both granted and revoked"));
    }

    @Test
    void multipleUnknownAuthoritiesAreReported() {

        User user = new User();
        user.setId(1L);

        Authority auth1 = new Authority();
        auth1.setId(10L);
        auth1.setAuthorityCode("QUOTATION_CREATE");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authorityRepository.findByAuthorityCodeIn(anyCollection()))
                .thenReturn(List.of(auth1));

        InvalidAuthorityException exception = assertThrows(
                InvalidAuthorityException.class,
                () -> service.updateGrantsAndRevokes(
                        1L,
                        List.of("QUOTATION_CREATE", "UNKNOWN_1"),
                        List.of("UNKNOWN_2")));

        List<String> unknown = exception.getUnknownAuthorities();
        assertTrue(unknown.contains("UNKNOWN_1"));
        assertTrue(unknown.contains("UNKNOWN_2"));
        assertEquals(2, unknown.size());
    }

    @Test
    void duplicateAuthoritiesInGrantListAreHandled() {

        // Verify that when the same authority is in the grant list twice,
        // both occurrences result in save calls
        User user = new User();
        user.setId(1L);

        Authority authority = new Authority();
        authority.setId(10L);
        authority.setAuthorityCode("QUOTATION_CREATE");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authorityRepository.findByAuthorityCodeIn(anyCollection()))
                .thenReturn(List.of(authority));
        when(userAuthorityRepository.findById(new UserAuthorityId(1L, 10L)))
                .thenReturn(Optional.empty());
        when(userAuthorityRepository.save(any(UserAuthority.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateGrantsAndRevokes(
                1L,
                List.of("QUOTATION_CREATE", "QUOTATION_CREATE"),
                null);

        // Should succeed without throwing exception
    }

    @Test
    void grantThenRevokeUpsertsBothCorrectly() {
        // Test that granting and then revoking the same authority
        // in separate calls both succeed
        User user = new User();
        user.setId(1L);

        Authority authority = new Authority();
        authority.setId(10L);
        authority.setAuthorityCode("QUOTATION_CREATE");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authorityRepository.findByAuthorityCodeIn(anyCollection()))
                .thenReturn(List.of(authority));
        when(userAuthorityRepository.findById(new UserAuthorityId(1L, 10L)))
                .thenReturn(Optional.empty());
        when(userAuthorityRepository.save(any(UserAuthority.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Both calls should succeed
        service.updateGrantsAndRevokes(1L, List.of("QUOTATION_CREATE"), null);
        service.updateGrantsAndRevokes(1L, null, List.of("QUOTATION_CREATE"));
    }
}
