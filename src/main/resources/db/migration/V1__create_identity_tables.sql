-- =========================================================
-- V1: Identity and Access Management Tables
-- =========================================================


-- =========================================================
-- 1. USER GROUP
-- =========================================================

CREATE TABLE user_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_code VARCHAR(50) NOT NULL UNIQUE,
    group_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);


-- =========================================================
-- 2. AUTHORITY
-- =========================================================

CREATE TABLE authority (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    authority_code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);


-- =========================================================
-- 3. USER GROUP AUTHORITY
-- =========================================================

CREATE TABLE user_group_authority (
    user_group_id BIGINT NOT NULL,
    authority_id BIGINT NOT NULL,

    PRIMARY KEY (user_group_id, authority_id),

    CONSTRAINT fk_uga_user_group
        FOREIGN KEY (user_group_id)
        REFERENCES user_group(id),

    CONSTRAINT fk_uga_authority
        FOREIGN KEY (authority_id)
        REFERENCES authority(id)
);


-- =========================================================
-- 4. USERS
-- =========================================================

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);


-- =========================================================
-- 5. USER GROUP MAPPING
-- =========================================================

CREATE TABLE user_group_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    user_group_id BIGINT NOT NULL,

    effective_from DATE NOT NULL,
    effective_to DATE NULL,

    CONSTRAINT uq_user_group_mapping
        UNIQUE (user_id, user_group_id, effective_from),

    CONSTRAINT fk_ugm_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_ugm_user_group
        FOREIGN KEY (user_group_id)
        REFERENCES user_group(id)
);


-- =========================================================
-- 6. USER AUTHORITY
-- =========================================================

CREATE TABLE user_authority (
    user_id BIGINT NOT NULL,
    authority_id BIGINT NOT NULL,

    effect ENUM('GRANT', 'REVOKE') NOT NULL,

    PRIMARY KEY (user_id, authority_id),

    CONSTRAINT fk_ua_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_ua_authority
        FOREIGN KEY (authority_id)
        REFERENCES authority(id)
);