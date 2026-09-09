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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAuthorityService {

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final UserAuthorityRepository userAuthorityRepository;

    public UserAuthorityService(
            UserRepository userRepository,
            AuthorityRepository authorityRepository,
            UserAuthorityRepository userAuthorityRepository) {

        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.userAuthorityRepository = userAuthorityRepository;
    }

    /**
     * Applies GRANT/REVOKE changes for one user.
     *
     * @Transactional: one call can touch several user_authority
     *                 rows. All-or-nothing — we don't want half the grants/revokes
     *                 applied if something fails partway.
     *
     *                 We upsert: if a (user_id, authority_id) row already exists
     *                 we update its effect instead of inserting a second row, so
     *                 the composite PK is never violated and re-granting/
     *                 re-revoking is idempotent.
     */
    @Transactional
    public void updateGrantsAndRevokes(Long userId, List<String> grantCodes, List<String> revokeCodes) {

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND",
                        "User not found: " + userId));

        List<String> grants = grantCodes == null ? List.of() : grantCodes;
        List<String> revokes = revokeCodes == null ? List.of() : revokeCodes;

        Set<String> overlap = new HashSet<>(grants);
        overlap.retainAll(revokes);

        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException(
                    "Authority cannot be both granted and revoked in the same request: " + overlap);
        }

        Set<String> allCodes = new HashSet<>();
        allCodes.addAll(grants);
        allCodes.addAll(revokes);

        if (allCodes.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one grant or revoke authority must be provided");
        }

        List<Authority> found = authorityRepository.findByAuthorityCodeIn(allCodes);

        if (found.size() != allCodes.size()) {

            Set<String> foundCodes = found.stream()
                    .map(Authority::getAuthorityCode)
                    .collect(Collectors.toSet());

            List<String> unknown = allCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .toList();

            throw new InvalidAuthorityException(unknown);
        }

        Map<String, Long> codeToId = found.stream()
                .collect(Collectors.toMap(Authority::getAuthorityCode, Authority::getId));

        for (String code : grants) {
            upsert(userId, codeToId.get(code), UserAuthority.Effect.GRANT);
        }

        for (String code : revokes) {
            upsert(userId, codeToId.get(code), UserAuthority.Effect.REVOKE);
        }
    }

    private void upsert(Long userId, Long authorityId, UserAuthority.Effect effect) {

        UserAuthorityId id = new UserAuthorityId(userId, authorityId);

        UserAuthority userAuthority = userAuthorityRepository.findById(id)
                .orElseGet(() -> {
                    UserAuthority created = new UserAuthority();
                    created.setId(id);
                    return created;
                });

        userAuthority.setEffect(effect);

        userAuthorityRepository.save(userAuthority);
    }
}