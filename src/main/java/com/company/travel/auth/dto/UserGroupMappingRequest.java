package com.company.travel.auth.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class UserGroupMappingRequest {

    @NotNull
    private Long userGroupId;

    @NotNull
    private LocalDate effectiveFrom;

    public UserGroupMappingRequest() {
    }

    public Long getUserGroupId() {
        return userGroupId;
    }

    public void setUserGroupId(Long userGroupId) {
        this.userGroupId = userGroupId;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }
}