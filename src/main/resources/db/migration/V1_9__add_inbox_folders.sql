CREATE TABLE inbox_folder (
  id                             BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name                           VARCHAR(255) NOT NULL
);

ALTER TABLE mail ADD COLUMN inbox_folder_id BIGINT NULL,
                 ADD CONSTRAINT fk_mail_inbox_folder FOREIGN KEY (inbox_folder_id) REFERENCES inbox_folder(id);