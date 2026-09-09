CREATE TABLE quotation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quotation_number VARCHAR(20) NOT NULL UNIQUE,
    traveller_name VARCHAR(200) NOT NULL,
    traveller_date_of_birth DATE NOT NULL,
    passport_number VARCHAR(100) NOT NULL,
    origin_country CHAR(2) NOT NULL,
    destination_country CHAR(2) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    travel_start_date DATE NOT NULL,
    travel_end_date DATE NOT NULL,
    cover_type VARCHAR(100) NOT NULL,
    sum_insured DECIMAL(19, 4) NOT NULL,
    premium DECIMAL(19, 4) NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_quotation_created_by_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
);

CREATE INDEX idx_quotation_owner_status_created
    ON quotation(created_by_user_id, status, created_at);