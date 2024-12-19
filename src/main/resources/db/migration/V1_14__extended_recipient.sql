ALTER TABLE recipient
    ADD full_organisation_name VARCHAR(255) NOT NULL;

ALTER TABLE recipient
    RENAME COLUMN organisation_name TO short_organisation_name;

UPDATE recipient
SET full_organisation_name = short_organisation_name;

ALTER TABLE recipient
MODIFY short_organisation_name VARCHAR(35) NOT NULL;