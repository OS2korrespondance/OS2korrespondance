
drop table attachment;

ALTER TABLE mail
    ADD binary_id BIGINT NULL;

CREATE TABLE `binary`
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    message_id     BIGINT                NULL,
    s3_file_key    VARCHAR(255)          NULL,
    identifier     VARCHAR(255)          NULL,
    code           VARCHAR(255)          NULL,
    extension_code VARCHAR(255)          NULL,
    original_size  BIGINT                NULL,
    CONSTRAINT pk_binary PRIMARY KEY (id)
);

CREATE TABLE binary_message
(
    id                  BIGINT AUTO_INCREMENT NOT NULL,
    sender_id           BIGINT                NULL,
    recipient_id        BIGINT                NULL,
    patient_id          BIGINT                NULL,
    received            datetime              NULL,
    envelope_identifier VARCHAR(255)          NULL,
    letter_identifier   VARCHAR(255)          NULL,
    incomming           BIT(1)                NULL,
    CONSTRAINT pk_binary_message PRIMARY KEY (id)
);

CREATE TABLE `reference`
(
    id                    BIGINT AUTO_INCREMENT NOT NULL,
    object_identifier     VARCHAR(255)          NULL,
    reference_type        VARCHAR(255)          NULL,
    filename              VARCHAR(768)          NULL,
    reference_description VARCHAR(255)          NULL,
    reference_url         VARCHAR(255)          NULL,
    object_code           VARCHAR(255)          NULL,
    object_extension_code VARCHAR(255)          NULL,
    object_original_size  BIGINT                NULL,
    mail_id               BIGINT                NULL,
    CONSTRAINT pk_reference PRIMARY KEY (id)
);
ALTER TABLE `reference`
    ADD CONSTRAINT FK_REFERENCE_ON_MAIL FOREIGN KEY (mail_id) REFERENCES mail (id) ON DELETE CASCADE ;