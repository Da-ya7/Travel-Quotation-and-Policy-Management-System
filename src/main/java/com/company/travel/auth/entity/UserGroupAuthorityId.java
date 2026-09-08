package com.company.travel.auth.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserGroupAuthorityId implements Serializable {

    private Long userGroupId;

    private Long authorityId;

    public UserGroupAuthorityId() {
    }

    public UserGroupAuthorityId(Long userGroupId, Long authorityId) {
        this.userGroupId = userGroupId;
        this.authorityId = authorityId;
    }

    public Long getUserGroupId() {
        return userGroupId;
    }

    public void setUserGroupId(Long userGroupId) {
        this.userGroupId = userGroupId;
    }

    public Long getAuthorityId() {
        return authorityId;
    }

    public void setAuthorityId(Long authorityId) {
        this.authorityId = authorityId;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof UserGroupAuthorityId)) {
            return false;
        }

        UserGroupAuthorityId that = (UserGroupAuthorityId) o;

        return Objects.equals(userGroupId, that.userGroupId)
                && Objects.equals(authorityId, that.authorityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userGroupId, authorityId);
    }
}