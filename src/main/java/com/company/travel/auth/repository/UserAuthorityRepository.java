package com.company.travel.auth.repository;

import com.company.travel.auth.entity.UserAuthority;
import com.company.travel.auth.entity.UserAuthorityId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAuthorityRepository
        extends JpaRepository<UserAuthority, UserAuthorityId> {

    List<UserAuthority> findByIdUserId(Long userId);
}