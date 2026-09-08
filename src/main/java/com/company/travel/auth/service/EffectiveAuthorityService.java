package com.company.travel.auth.service;

import com.company.travel.auth.entity.UserAuthority;
import com.company.travel.auth.entity.UserGroupMapping;
import com.company.travel.auth.repository.UserAuthorityRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EffectiveAuthorityService {

    private final UserGroupService userGroupService;
    private final AuthorityService authorityService;
    private final UserAuthorityRepository userAuthorityRepository;

    public EffectiveAuthorityService(
            UserGroupService userGroupService,
            AuthorityService authorityService,
            UserAuthorityRepository userAuthorityRepository) {

        this.userGroupService = userGroupService;
        this.authorityService = authorityService;
        this.userAuthorityRepository = userAuthorityRepository;
    }

    public Set<String> getEffectiveAuthorities(Long userId) {

        Set<String> effectiveAuthorities = new HashSet<>();

        // 1. Find the user's active group mapping
        UserGroupMapping mapping = userGroupService.findActiveGroupMapping(userId);

        // No active mapping means the user has no group authorities
        if (mapping == null) {
            return effectiveAuthorities;
        }

        // 2. Add authorities from the user's group
        List<String> groupAuthorities = authorityService.getAuthoritiesForGroup(
                mapping.getUserGroupId());

        effectiveAuthorities.addAll(groupAuthorities);

        // 3. Apply user-level GRANT and REVOKE
        List<UserAuthority> userAuthorities = userAuthorityRepository.findByIdUserId(userId);

        for (UserAuthority userAuthority : userAuthorities) {

            String authorityCode = authorityService.getAuthorityCode(
                    userAuthority.getId().getAuthorityId());

            if (authorityCode == null) {
                continue;
            }

            if (userAuthority.getEffect() == UserAuthority.Effect.GRANT) {

                effectiveAuthorities.add(authorityCode);

            } else if (userAuthority.getEffect() == UserAuthority.Effect.REVOKE) {

                effectiveAuthorities.remove(authorityCode);
            }
        }

        return effectiveAuthorities;
    }

    public boolean hasActiveGroupMapping(Long userId) {

        UserGroupMapping mapping = userGroupService.findActiveGroupMapping(userId);

        return mapping != null;
    }
}