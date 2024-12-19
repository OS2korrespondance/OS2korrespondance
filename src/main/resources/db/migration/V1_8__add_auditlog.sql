CREATE TABLE auditlog (
  id                             BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  tts                            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip                             VARCHAR(255) NOT NULL,
  performed_by                   VARCHAR(255) NOT NULL,
  operation                      VARCHAR(255) NOT NULL,
  details                        TEXT
);