-- ============================================================
-- V2: Seed identity and authorization catalogue
-- ============================================================


-- ============================================================
-- 1. Authorities
-- ============================================================

INSERT INTO authority (authority_code, description)
VALUES
    ('USER_GROUP_CREATE', 'Create and manage user groups'),
    ('USER_CREATE', 'Create users'),
    ('USER_MAP_GROUP', 'Map users to user groups'),
    ('QUOTATION_CREATE', 'Create travel quotations'),
    ('QUOTATION_VIEW_OWN', 'View own quotations'),
    ('PAYMENT_COLLECT', 'Collect quotation payments'),
    ('DOCUMENT_UPLOAD', 'Upload quotation documents'),
    ('QUOTATION_CONVERT_POLICY', 'Convert quotation to policy'),
    ('POLICY_VIEW_OWN', 'View own policies'),
    ('POLICY_VIEW_REFERRED', 'View referred policies'),
    ('POLICY_APPROVE_WAR', 'Approve war-geography policies'),
    ('POLICY_REJECT_WAR', 'Reject war-geography policies'),
    ('WAR_GEOGRAPHY_MAINTAIN', 'Maintain war-geography master'),
    ('AUDIT_VIEW', 'View audit logs');


-- ============================================================
-- 2. User Groups
-- ============================================================

INSERT INTO user_group
    (group_code, group_name, status, created_at, updated_at)
VALUES
    (
        'UNDERWRITING_USER',
        'Underwriting user',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'APPROVER_USER',
        'Approver user',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'SYSTEM_ADMIN',
        'System administrator',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );


-- ============================================================
-- 3. UNDERWRITING_USER authorities
-- ============================================================

INSERT INTO user_group_authority
    (user_group_id, authority_id)
SELECT
    ug.id,
    a.id
FROM user_group ug
JOIN authority a
    ON a.authority_code IN (
        'QUOTATION_CREATE',
        'QUOTATION_VIEW_OWN',
        'PAYMENT_COLLECT',
        'DOCUMENT_UPLOAD',
        'QUOTATION_CONVERT_POLICY',
        'POLICY_VIEW_OWN'
    )
WHERE ug.group_code = 'UNDERWRITING_USER';


-- ============================================================
-- 4. APPROVER_USER authorities
-- ============================================================

INSERT INTO user_group_authority
    (user_group_id, authority_id)
SELECT
    ug.id,
    a.id
FROM user_group ug
JOIN authority a
    ON a.authority_code IN (
        'POLICY_VIEW_REFERRED',
        'POLICY_APPROVE_WAR',
        'POLICY_REJECT_WAR'
    )
WHERE ug.group_code = 'APPROVER_USER';


-- ============================================================
-- 5. SYSTEM_ADMIN authorities
-- ============================================================

INSERT INTO user_group_authority
    (user_group_id, authority_id)
SELECT
    ug.id,
    a.id
FROM user_group ug
JOIN authority a
    ON a.authority_code IN (
        'USER_GROUP_CREATE',
        'USER_CREATE',
        'USER_MAP_GROUP',
        'WAR_GEOGRAPHY_MAINTAIN',
        'AUDIT_VIEW'
    )
WHERE ug.group_code = 'SYSTEM_ADMIN';