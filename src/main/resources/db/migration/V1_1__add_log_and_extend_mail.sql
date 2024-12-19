CREATE TABLE mail_receipt_log (
    id                                BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    receipt_xml                       TEXT NULL,
    mail_xml                          TEXT NULL,
    mail_tts                          TIMESTAMP NULL,
    receipt_tts                       TIMESTAMP NULL,
    receipt_type                      VARCHAR(255) NULL,
    negative_receipt_reason           VARCHAR(350) NULL,
    mail_envelope_identifier          VARCHAR(14) NULL,
    mail_letter_identifier            VARCHAR(14) NULL,
    receipt_s3_file_key               VARCHAR(500) NULL,
    incomming_mail                    BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE mail ADD COLUMN high_priority_mail BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE mail ADD COLUMN envelope_identifier VARCHAR(14) NULL;
ALTER TABLE mail ADD COLUMN letter_identifier VARCHAR(14) NULL;
ALTER TABLE mail ADD COLUMN received_negative_receipt BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE mail ADD COLUMN negative_receipt_reason VARCHAR(350) NULL;

DROP TABLE setting;
