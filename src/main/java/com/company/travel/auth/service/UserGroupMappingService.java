package com.company.travel.auth.service;

import com.company.travel.auth.entity.User;
import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.entity.UserGroupMapping;
import com.company.travel.auth.exception.InactiveGroupException;
import com.company.travel.auth.exception.ResourceNotFoundException;
import com.company.travel.auth.repository.UserGroupMappingRepository;
import com.company.travel.auth.repository.UserGroupRepository;
import com.company.travel.auth.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UserGroupMappingService {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupMappingRepository mappingRepository;

    public UserGroupMappingService(
            UserRepository userRepository,
            UserGroupRepository userGroupRepository,
            UserGroupMappingRepository mappingRepository) {

        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.mappingRepository = mappingRepository;
    }

    /**
     * Maps a user to a group starting effectiveFrom.
     *
     * @Transactional: closing the old open mapping and inserting
     *                 the new one are two writes. If we crashed between them
     *                 we'd risk zero or two active mappings. One transaction
     *                 means both happen or neither does.
     *
     *                 Overlap safety: v1 allows one open (effective_to = NULL)
     *                 mapping per user. We find that row via findOpenMapping()
     *                 and close it the day BEFORE the new mapping starts
     *                 (effectiveFrom - 1 day) — since findActiveMapping treats
     *                 effective_to as inclusive, this guarantees the two ranges
     *                 never overlap on the same day.
     */
    @Transactional
    public UserGroupMapping mapUserToGroup(Long userId, Long userGroupId, LocalDate effectiveFrom) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND",
                        "User not found: " + userId));

        UserGroup group = userGroupRepository.findById(userGroupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "GROUP_NOT_FOUND",
                        "User group not found: " + userGroupId));

        if (!"ACTIVE".equalsIgnoreCase(group.getStatus())) {
            throw new InactiveGroupException(group.getGroupCode());
        }

        mappingRepository.findOpenMapping(userId).ifPresent(previous -> {

            LocalDate closeDate = effectiveFrom.minusDays(1);

            if (closeDate.isBefore(previous.getEffectiveFrom())) {
                throw new IllegalArgumentException(
                        "effectiveFrom must be after the current mapping's effectiveFrom ("
                                + previous.getEffectiveFrom() + ")");
            }

            previous.setEffectiveTo(closeDate);
            mappingRepository.save(previous);
        });

        UserGroupMapping newMapping = new UserGroupMapping();
        newMapping.setUserId(user.getId());
        newMapping.setUserGroupId(group.getId());
        newMapping.setEffectiveFrom(effectiveFrom);
        newMapping.setEffectiveTo(null);

        return mappingRepository.save(newMapping);
    }
}