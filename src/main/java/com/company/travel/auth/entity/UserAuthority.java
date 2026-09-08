package com.company.travel.auth.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_authority")
public class UserAuthority {

    @EmbeddedId
    private UserAuthorityId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Effect effect;

    public UserAuthority() {
    }

    public UserAuthorityId getId() {
        return id;
    }

    public void setId(UserAuthorityId id) {
        this.id = id;
    }

    public Effect getEffect() {
        return effect;
    }

    public void setEffect(Effect effect) {
        this.effect = effect;
    }

    public enum Effect {
        GRANT,
        REVOKE
    }
}