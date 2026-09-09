package com.company.travel.auth.dto;

import java.util.List;

// No @NotEmpty here — grants and revokes are individually optional,
// but at least one of them must be non-empty. That's a cross-field
// rule, so it's validated in UserAuthorityService, not here.
public class UserAuthorityUpdateRequest {

    private List<String> grants;
    private List<String> revokes;

    public UserAuthorityUpdateRequest() {
    }

    public List<String> getGrants() {
        return grants;
    }

    public void setGrants(List<String> grants) {
        this.grants = grants;
    }

    public List<String> getRevokes() {
        return revokes;
    }

    public void setRevokes(List<String> revokes) {
        this.revokes = revokes;
    }
}