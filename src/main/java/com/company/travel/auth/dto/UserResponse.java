package com.company.travel.auth.dto;

public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String status;

    public UserResponse() {
    }

    public UserResponse(
            Long id,
            String username,
            String email,
            String fullName,
            String status) {

        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getStatus() {
        return status;
    }
}