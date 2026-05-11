CREATE TABLE user_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(255) NOT NULL,
    signature TEXT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_profiles_user_id (user_id)
);