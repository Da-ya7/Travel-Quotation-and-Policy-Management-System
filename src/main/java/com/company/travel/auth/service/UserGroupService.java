package com.company.travel.auth.service;

import com.company.travel.auth.entity.Authority;
import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.entity.UserGroupAuthority;
import com.company.travel.auth.entity.UserGroupAuthorityId;
import com.company.travel.auth.entity.UserGroupMapping;
import com.company.travel.auth.exception.GroupAlreadyExistsException;
import com.company.travel.auth.exception.InvalidAuthorityException;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.repository.AuthorityRepository;
import com.company.travel.auth.repository.UserGroupAuthorityRepository;
import com.company.travel.auth.repository.UserGroupMappingRepository;
import com.company.travel.auth.repository.UserGroupRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserGroupService {

    private final UserGroupMappingRepository mappingRepository;
    private final UserGroupRepository userGroupRepository;
    private final AuthorityRepository authorityRepository;
    private final UserGroupAuthorityRepository userGroupAuthorityRepository;

    public UserGroupService(
            UserGroupMappingRepository mappingRepository,
            UserGroupRepository userGroupRepository,
            AuthorityRepository authorityRepository,
            UserGroupAuthorityRepository userGroupAuthorityRepository) {

        this.mappingRepository = mappingRepository;
        this.userGroupRepository = userGroupRepository;
        this.authorityRepository = authorityRepository;
        this.userGroupAuthorityRepository = userGroupAuthorityRepository;
    }

    // =========================================================
    // Phase 3 — unchanged
    // =========================================================

    public UserGroupMapping findActiveGroupMapping(Long userId) {

        UserGroupMapping mapping = mappingRepository
                .findActiveMapping(userId, LocalDate.now())
                .orElse(null);

        if (mapping == null) {
            return null;
        }

        UserGroup group = findGroupById(mapping.getUserGroupId());

        if (group == null) {
            return null;
        }

        if (!"ACTIVE".equalsIgnoreCase(group.getStatus())) {
            return null;
        }

        return mapping;
    }

    public UserGroup findGroupById(Long userGroupId) {

        return userGroupRepository
                .findById(userGroupId)
                .orElse(null);
    }

    // =========================================================
    // Phase 4 — admin group management
    // =========================================================

    /**
     * Creates a new group together with its initial authorities.
     *
     * @Transactional because this is two writes (user_group,
     *                then user_group_authority rows). If the second write fails,
     *                we don't want an orphan group with zero authorities sitting
     *                in the DB — the whole method rolls back together.
     */
    @Transactional
    public UserGroup createGroup(
            String groupCode,
            String groupName,
            String status,
            List<String> authorityCodes) {

        if (userGroupRepository.existsByGroupCode(groupCode)) {
            throw new GroupAlreadyExistsException(groupCode);
        }

        List<Authority> authorities = resolveAuthorities(authorityCodes);

        UserGroup group = new UserGroup();
        group.setGroupCode(groupCode);
        group.setGroupName(groupName);
        group.setStatus(status);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());

        group = userGroupRepository.save(group);

        linkAuthorities(group.getId(), authorities);

        return group;
    }

    /**
     * Adds authorities to an existing group. Authorities already
     * linked are silently skipped — we never insert a duplicate
     * (user_group_id, authority_id) row.
     */
    @Transactional
    public UserGroup assignAuthorities(Long groupId, List<String> authorityCodes) {

        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "GROUP_NOT_FOUND",
                        "User group not found: " + groupId));

        List<Authority> authorities = resolveAuthorities(authorityCodes);

        Set<Long> alreadyLinked = userGroupAuthorityRepository
                .findByIdUserGroupId(groupId)
                .stream()
                .map(link -> link.getId().getAuthorityId())
                .collect(Collectors.toSet());

        List<Authority> toLink = authorities.stream()
                .filter(authority -> !alreadyLinked.contains(authority.getId()))
                .toList();

        linkAuthorities(groupId, toLink);

        return group;
    }

    public List<UserGroup> findAllGroups() {
        return userGroupRepository.findAll();
    }

    public List<String> getAuthorityCodesForGroup(Long groupId) {

        return userGroupAuthorityRepository.findByIdUserGroupId(groupId)
                .stream()
                .map(link -> authorityRepository.findById(link.getId().getAuthorityId())
                        .map(Authority::getAuthorityCode)
                        .orElse(null))
                .filter(code -> code != null)
                .toList();
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    // Looks up every requested code against the authority
    // catalogue. Any code that doesn't exist throws — we never
    // silently ignore a typo'd authority code.
    private List<Authority> resolveAuthorities(List<String> authorityCodes) {

        List<String> distinctCodes = authorityCodes.stream().distinct().toList();

        List<Authority> found = authorityRepository.findByAuthorityCodeIn(distinctCodes);

        if (found.size() != distinctCodes.size()) {

            Set<String> foundCodes = found.stream()
                    .map(Authority::getAuthorityCode)
                    .collect(Collectors.toSet());

            List<String> unknown = distinctCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .toList();

            throw new InvalidAuthorityException(unknown);
        }

        return found;
    }

    private void linkAuthorities(Long groupId, List<Authority> authorities) {

        List<UserGroupAuthority> links = new ArrayList<>();

        for (Authority authority : authorities) {

            UserGroupAuthority link = new UserGroupAuthority();
            link.setId(new UserGroupAuthorityId(groupId, authority.getId()));
            links.add(link);
        }

        userGroupAuthorityRepository.saveAll(links);
    }
}