package com.company.travel.auth.service;

import com.company.travel.auth.entity.UserGroup;
import com.company.travel.auth.entity.UserGroupMapping;
import com.company.travel.auth.repository.UserGroupMappingRepository;
import com.company.travel.auth.repository.UserGroupRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UserGroupService {

    private final UserGroupMappingRepository mappingRepository;
    private final UserGroupRepository userGroupRepository;

    public UserGroupService(
            UserGroupMappingRepository mappingRepository,
            UserGroupRepository userGroupRepository) {

        this.mappingRepository = mappingRepository;
        this.userGroupRepository = userGroupRepository;
    }

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
}