package com.company.travel.auth.repository;

import com.company.travel.auth.entity.UserGroupMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UserGroupMappingRepository
                extends JpaRepository<UserGroupMapping, Long> {

        // Unchanged from Phase 3 — used by login/JWT to check "is this
        // user allowed in right now" as of a given date.
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

        // New for Phase 4: finds the current OPEN-ENDED mapping
        // (effective_to IS NULL) regardless of date. This is the row
        // we must close when mapping the user to a new group — v1
        // allows only one open mapping per user at a time.
        @Query("""
                            SELECT m
                            FROM UserGroupMapping m
                            WHERE m.userId = :userId
                              AND m.effectiveTo IS NULL
                        """)
        Optional<UserGroupMapping> findOpenMapping(
                        @Param("userId") Long userId);
}