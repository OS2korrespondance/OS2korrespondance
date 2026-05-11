
CREATE TABLE monitoring_state (
    id BIGINT NOT NULL DEFAULT 1,
    last_seen DATETIME NULL,
    uptime_seconds BIGINT NULL,
    previous_uptime_seconds BIGINT NULL,
    paths_json TEXT NULL,
    previous_paths_json TEXT NULL,
    last_alarm_sent DATETIME NULL,
    alarm_interval_minutes INT NOT NULL DEFAULT 180,
    last_error_alarm_sent DATETIME NULL,
    last_error_alarm_key VARCHAR(255) NULL,
    PRIMARY KEY (id)
);

INSERT INTO monitoring_state (id, alarm_interval_minutes) VALUES (1, 180);

CREATE TABLE monitoring_subscriber (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    subscriber_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
);
 