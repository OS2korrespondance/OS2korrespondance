ALTER TABLE inbox ADD COLUMN auto_delete_enabled BIT(1) NOT NULL DEFAULT 0;
ALTER TABLE inbox ADD COLUMN auto_delete_days BIGINT NULL;