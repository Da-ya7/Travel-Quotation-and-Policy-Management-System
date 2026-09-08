package com.company.travel.auth.dto;

import java.util.Set;

public class MeResponse {

    private Long userId;
    private String username;
    private Long userGroupId;
    private String userGroupCode;
    private Set<String> authorities;

    public MeResponse() {
    }

    public MeResponse(
            Long userId,
            String username,
            Long userGroupId,
            String userGroupCode,
            Set<String> authorities) {

        this.userId = userId;
        this.username = username;
        this.userGroupId = userGroupId;
        this.userGroupCode = userGroupCode;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getUserGroupId() {
        return userGroupId;
    }

    public String getUserGroupCode() {
        return userGroupCode;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }
}