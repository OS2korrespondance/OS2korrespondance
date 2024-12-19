ALTER TABLE mail ADD COLUMN original_folder VARCHAR(255) NULL,
                 ADD COLUMN deleted_date TIMESTAMP NULL;

UPDATE mail m SET m.original_folder = case m.folder
        when 'ARCHIVE' then 'INBOX'
        when 'INBOX' then 'INBOX'
        when 'SENT' then 'SENT'
    end;

ALTER TABLE mail MODIFY original_folder VARCHAR(255) NOT NULL;

