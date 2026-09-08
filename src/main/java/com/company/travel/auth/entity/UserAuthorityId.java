package com.company.travel.auth.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserAuthorityId implements Serializable {

    private Long userId;

    private Long authorityId;

    public UserAuthorityId() {
    }

    public UserAuthorityId(Long userId, Long authorityId) {
        this.userId = userId;
        this.authorityId = authorityId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

        if (!(o instanceof UserAuthorityId)) {
            return false;
        }

        UserAuthorityId that = (UserAuthorityId) o;

        return Objects.equals(userId, that.userId)
                && Objects.equals(authorityId, that.authorityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, authorityId);
    }
}