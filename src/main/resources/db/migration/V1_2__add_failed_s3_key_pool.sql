CREATE TABLE failed_s3_key_pool (
   id                                BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
   s3_file_key                       VARCHAR(500) NULL
);