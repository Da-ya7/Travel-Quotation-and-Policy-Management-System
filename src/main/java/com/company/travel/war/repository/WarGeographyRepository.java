package com.company.travel.war.repository;

import com.company.travel.war.entity.WarGeography;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WarGeographyRepository extends JpaRepository<WarGeography, Long> {

    @Query("select case when count(g) > 0 then true else false end "
            + "from WarGeography g where g.countryCode = :countryCode "
            + "and ((:regionOrCity is null and g.regionOrCity is null) "
            + "or g.regionOrCity = :regionOrCity)")
    boolean existsByCountryCodeAndRegionOrCity(
            @Param("countryCode") String countryCode,
            @Param("regionOrCity") String regionOrCity);

    List<WarGeography> findAllByActiveTrue();
}