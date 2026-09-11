CREATE TABLE policy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_number VARCHAR(20) NOT NULL UNIQUE,
    quotation_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    requires_approval BOOLEAN NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    issued_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_policy_quotation
        FOREIGN KEY (quotation_id) REFERENCES quotation(id),
    CONSTRAINT fk_policy_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_policy_owner_status
    ON policy(created_by_user_id, status);