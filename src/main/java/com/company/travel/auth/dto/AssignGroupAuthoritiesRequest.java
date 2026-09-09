package com.company.travel.auth.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AssignGroupAuthoritiesRequest {

    @NotEmpty(message = "at least one authority is required")
    private List<String> authorities;

    public AssignGroupAuthoritiesRequest() {
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }
}