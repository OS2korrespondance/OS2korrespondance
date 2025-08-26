
DROP TABLE IF EXISTS subscriber;

CREATE TABLE inbox
(
    id                            BIGINT AUTO_INCREMENT NOT NULL,
    ean_identifier                VARCHAR(255)          NULL,
    negative_receipt_notification BIT(1)                NULL,
    CONSTRAINT pk_inbox PRIMARY KEY (id)
);

CREATE TABLE inbox_subscriber
(
    id       BIGINT AUTO_INCREMENT NOT NULL,
    email    VARCHAR(255)          NULL,
    inbox_id BIGINT                NULL,
    CONSTRAINT pk_inbox_subscriber PRIMARY KEY (id)
);

ALTER TABLE inbox_subscriber
    ADD CONSTRAINT uc_email_ean UNIQUE (email, inbox_id);

ALTER TABLE inbox_subscriber
    ADD CONSTRAINT FK_INBOX_SUBSCRIBER_ON_INBOX FOREIGN KEY (inbox_id) REFERENCES inbox (id);
