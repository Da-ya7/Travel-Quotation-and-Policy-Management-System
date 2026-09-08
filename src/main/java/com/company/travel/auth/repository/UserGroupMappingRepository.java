package com.company.travel.auth.repository;

import com.company.travel.auth.entity.UserGroupMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UserGroupMappingRepository
        extends JpaRepository<UserGroupMapping, Long> {

    @Query("""
                SELECT m
                FROM UserGroupMapping m
                WHERE m.userId = :userId
                  AND m.effectiveFrom <= :today
                  AND (m.effectiveTo IS NULL OR m.effectiveTo >= :today)
            """)
    Optional<UserGroupMapping> findActiveMapping(
            @Param("userId") Long userId,
            @Param("today") LocalDate today);
}