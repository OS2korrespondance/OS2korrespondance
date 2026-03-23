ALTER TABLE inbox ADD COLUMN auto_reply_enabled BIT(1) NOT NULL DEFAULT 0;
ALTER TABLE inbox ADD COLUMN auto_reply_subject VARCHAR(255) NULL;
ALTER TABLE inbox ADD COLUMN auto_reply_message TEXT NULL;
ALTER TABLE inbox ADD COLUMN auto_reply_start_date DATE NULL;
ALTER TABLE inbox ADD COLUMN auto_reply_end_date DATE NULL;
