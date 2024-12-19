CREATE TABLE patient (
    id                                BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    cpr                               VARCHAR(10) NULL,
    alternative_identifier            BOOLEAN NOT NULL DEFAULT FALSE,
    name                              VARCHAR(140) NOT NULL,
    episode_of_care_status_code       VARCHAR(255) NOT NULL
);

CREATE TABLE recipient (
    id                                BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    ean_identifier                    VARCHAR(35) NOT NULL,
    identifier                        VARCHAR(17) NOT NULL,
    identifier_code                   VARCHAR(255) NOT NULL,
    organisation_name                 VARCHAR(35) NOT NULL
);

CREATE TABLE mail (
    id                                BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    sender_id                         BIGINT NULL,
    recipient_id                      BIGINT NULL,
    subject                           VARCHAR(70) NOT NULL,
    content                           MEDIUMTEXT NOT NULL,
    folder                            VARCHAR(255) NOT NULL,
    status                            VARCHAR(255) NOT NULL,
    sent                              TIMESTAMP NULL,
    received                          TIMESTAMP NULL,
    read_mail                         BOOLEAN NOT NULL DEFAULT FALSE,
    draft                             BOOLEAN NOT NULL DEFAULT FALSE,
    created                           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    s3_file_key                       VARCHAR(500) NULL,
    answer_to                         BIGINT NULL,
    patient_id                        BIGINT NULL,
    
    CONSTRAINT fk_mail_sender FOREIGN KEY (sender_id) REFERENCES recipient(id) ON DELETE CASCADE,
    CONSTRAINT fk_mail_recipient FOREIGN KEY (recipient_id) REFERENCES recipient(id) ON DELETE CASCADE,
    CONSTRAINT fk_mail_answer_to FOREIGN KEY (answer_to) REFERENCES mail(id) ON DELETE CASCADE,
    CONSTRAINT fk_mail_patient FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE
);

CREATE TABLE attachment (
    id                                BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    filename                          VARCHAR(255) NULL,
    content                           MEDIUMBLOB NULL,
    mail_id                           BIGINT NOT NULL,
    type                              VARCHAR(3) NOT NULL,
    identifier                        VARCHAR(35) NULL,
    description                       VARCHAR(70) NULL,
    url                               VARCHAR(350) NULL,
    
    CONSTRAINT fk_attachment_mail FOREIGN KEY (mail_id) REFERENCES mail(id) ON DELETE CASCADE
);

CREATE TABLE setting (
    id                                BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    setting_key                       VARCHAR(64) NOT NULL,
    setting_value                     VARCHAR(255) NOT NULL
);