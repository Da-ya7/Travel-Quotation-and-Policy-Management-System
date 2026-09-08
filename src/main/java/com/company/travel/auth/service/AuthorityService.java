package com.company.travel.auth.service;

import com.company.travel.auth.entity.Authority;
import com.company.travel.auth.entity.UserGroupAuthority;
import com.company.travel.auth.repository.AuthorityRepository;
import com.company.travel.auth.repository.UserGroupAuthorityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorityService {

    private final AuthorityRepository authorityRepository;
    private final UserGroupAuthorityRepository userGroupAuthorityRepository;

    public AuthorityService(
            AuthorityRepository authorityRepository,
            UserGroupAuthorityRepository userGroupAuthorityRepository) {

        this.authorityRepository = authorityRepository;
        this.userGroupAuthorityRepository = userGroupAuthorityRepository;
    }

    public List<String> getAuthoritiesForGroup(Long userGroupId) {

        List<UserGroupAuthority> mappings = userGroupAuthorityRepository
                .findByIdUserGroupId(userGroupId);

        return mappings.stream()
                .map(mapping -> authorityRepository
                        .findById(mapping.getId().getAuthorityId())
                        .map(Authority::getAuthorityCode)
                        .orElse(null))
                .filter(code -> code != null)
                .toList();
    }

    public String getAuthorityCode(Long authorityId) {

        return authorityRepository
                .findById(authorityId)
                .map(Authority::getAuthorityCode)
                .orElse(null);
    }
}