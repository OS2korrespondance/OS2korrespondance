CREATE TABLE subscriber
(
    id                                BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    email                             VARCHAR(255) NOT NULL,
    inbox_ean                         VARCHAR(50) NOT NULL
);

ALTER TABLE subscriber
    ADD CONSTRAINT uc_email_ean UNIQUE (email, inbox_ean);
