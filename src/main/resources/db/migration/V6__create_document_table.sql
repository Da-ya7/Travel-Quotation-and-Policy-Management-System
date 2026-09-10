CREATE TABLE document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quotation_id BIGINT NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    storage_uri VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    extracted_json JSON NULL,
    validation_message VARCHAR(1000) NULL,
    uploaded_by_user_id BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_quotation
        FOREIGN KEY (quotation_id) REFERENCES quotation(id),
    CONSTRAINT fk_document_uploaded_by_user
        FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_document_quotation_status
    ON document(quotation_id, status);