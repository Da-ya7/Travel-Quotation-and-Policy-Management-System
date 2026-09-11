CREATE TABLE war_geography (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country_code CHAR(2) NOT NULL,
    region_or_city VARCHAR(100) NULL,
    related_to_war_geography_id BIGINT NULL,
    active BOOLEAN NOT NULL,

    CONSTRAINT uq_war_geography_country_region
        UNIQUE (country_code, region_or_city),
    CONSTRAINT fk_war_geography_related
        FOREIGN KEY (related_to_war_geography_id)
        REFERENCES war_geography(id)
);