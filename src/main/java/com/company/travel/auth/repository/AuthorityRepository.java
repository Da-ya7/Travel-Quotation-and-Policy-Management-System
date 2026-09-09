package com.company.travel.auth.repository;

import com.company.travel.auth.entity.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {

    Optional<Authority> findByAuthorityCode(String authorityCode);

    List<Authority> findByAuthorityCodeIn(Collection<String> authorityCodes);
}