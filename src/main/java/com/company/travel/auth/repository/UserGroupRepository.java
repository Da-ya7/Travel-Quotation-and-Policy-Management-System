package com.company.travel.auth.repository;

import com.company.travel.auth.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    Optional<UserGroup> findByGroupCode(String groupCode);

    boolean existsByGroupCode(String groupCode);
}