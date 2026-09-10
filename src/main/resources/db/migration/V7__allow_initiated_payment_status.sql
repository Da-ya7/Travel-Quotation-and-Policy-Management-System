ALTER TABLE payment DROP CONSTRAINT chk_payment_outcome;

ALTER TABLE payment
    ADD CONSTRAINT chk_payment_outcome
    CHECK (outcome IN ('INITIATED', 'SUCCESS', 'FAILED'));