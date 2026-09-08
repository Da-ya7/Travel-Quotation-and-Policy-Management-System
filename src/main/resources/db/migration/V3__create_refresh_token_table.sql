CREATE TABLE refresh_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT uk_refresh_token_hash
        UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_user
    ON refresh_token(user_id);

CREATE INDEX idx_refresh_token_expires
    ON refresh_token(expires_at);