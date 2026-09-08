package com.company.travel.auth.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_group_authority")
public class UserGroupAuthority {

    @EmbeddedId
    private UserGroupAuthorityId id;

    public UserGroupAuthority() {
    }

    public UserGroupAuthorityId getId() {
        return id;
    }

    public void setId(UserGroupAuthorityId id) {
        this.id = id;
    }
}