package com.company.travel.auth.repository;

import com.company.travel.auth.entity.UserGroupAuthority;
import com.company.travel.auth.entity.UserGroupAuthorityId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGroupAuthorityRepository
        extends JpaRepository<UserGroupAuthority, UserGroupAuthorityId> {

    List<UserGroupAuthority> findByIdUserGroupId(Long userGroupId);
}