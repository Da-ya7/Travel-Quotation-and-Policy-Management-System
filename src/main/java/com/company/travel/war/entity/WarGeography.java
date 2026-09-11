package com.company.travel.war.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "war_geography", uniqueConstraints = @UniqueConstraint(name = "uq_war_geography_country_region", columnNames = {
        "country_code", "region_or_city" }))
public class WarGeography {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "region_or_city", length = 100)
    private String regionOrCity;

    @Column(name = "related_to_war_geography_id")
    private Long relatedToWarGeographyId;

    @Column(nullable = false)
    private boolean active;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getRegionOrCity() {
        return regionOrCity;
    }

    public void setRegionOrCity(String regionOrCity) {
        this.regionOrCity = regionOrCity;
    }

    public Long getRelatedToWarGeographyId() {
        return relatedToWarGeographyId;
    }

    public void setRelatedToWarGeographyId(Long relatedToWarGeographyId) {
        this.relatedToWarGeographyId = relatedToWarGeographyId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}