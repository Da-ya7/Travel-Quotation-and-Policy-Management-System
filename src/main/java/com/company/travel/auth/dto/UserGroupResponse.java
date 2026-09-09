package com.company.travel.auth.dto;

import java.util.List;

public class UserGroupResponse {

    private Long id;
    private String groupCode;
    private String groupName;
    private String status;
    private List<String> authorities;

    public UserGroupResponse() {
    }

    public UserGroupResponse(
            Long id,
            String groupCode,
            String groupName,
            String status,
            List<String> authorities) {

        this.id = id;
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.status = status;
        this.authorities = authorities;
    }

    public Long getId() {
        return id;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getAuthorities() {
        return authorities;
    }
}