CREATE TABLE policy_referral (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL UNIQUE,
    referred_to_group_id BIGINT NOT NULL,
    assigned_approver_user_id BIGINT NULL,
    referred_by_user_id BIGINT NOT NULL,
    referral_status VARCHAR(20) NOT NULL,
    decided_by_user_id BIGINT NULL,
    decision_reason VARCHAR(500) NULL,
    referred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP NULL,

    CONSTRAINT fk_policy_referral_policy
        FOREIGN KEY (policy_id) REFERENCES policy(id),
    CONSTRAINT fk_policy_referral_group
        FOREIGN KEY (referred_to_group_id) REFERENCES user_group(id),
    CONSTRAINT fk_policy_referral_assigned_user
        FOREIGN KEY (assigned_approver_user_id) REFERENCES users(id),
    CONSTRAINT fk_policy_referral_referred_by_user
        FOREIGN KEY (referred_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_policy_referral_decided_by_user
        FOREIGN KEY (decided_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_policy_referral_assigned_status
    ON policy_referral(assigned_approver_user_id, referral_status);

CREATE INDEX idx_policy_referral_group_status
    ON policy_referral(referred_to_group_id, referral_status);