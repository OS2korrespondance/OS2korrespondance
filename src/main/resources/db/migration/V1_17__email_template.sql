CREATE TABLE message_template (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NULL,
    is_high_priority BOOLEAN NOT NULL DEFAULT FALSE,
    content TEXT NULL,
    INDEX idx_message_template_template_name (template_name)
);
