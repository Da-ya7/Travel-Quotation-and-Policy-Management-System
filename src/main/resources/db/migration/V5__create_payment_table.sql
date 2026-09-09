CREATE TABLE payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quotation_id BIGINT NOT NULL UNIQUE,
    outcome VARCHAR(10) NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_payment_quotation
        FOREIGN KEY (quotation_id)
        REFERENCES quotation(id),

    CONSTRAINT chk_payment_outcome
        CHECK (outcome IN ('SUCCESS', 'FAILED'))
);
