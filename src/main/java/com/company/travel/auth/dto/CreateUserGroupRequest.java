package com.company.travel.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public class CreateUserGroupRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "groupCode must be uppercase snake_case")
    private String groupCode;

    @NotBlank
    private String groupName;

    @NotBlank
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "status must be ACTIVE or INACTIVE")
    private String status;

    @NotEmpty(message = "at least one authority is required")
    private List<String> authorities;

    public CreateUserGroupRequest() {
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }
}