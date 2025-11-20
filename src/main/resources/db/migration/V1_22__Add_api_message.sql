CREATE TABLE api_message(
    id          BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    file_name   VARCHAR(255) NOT NULL,
    file_size   BIGINT(255) NOT NULL,
    group_id    VARCHAR(255) NOT NULL
);