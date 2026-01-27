CREATE TABLE alert_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    date DATE NOT NULL,
    hour INT NOT NULL,
    danger_behavior_id BIGINT NOT NULL,
    alert_count INT DEFAULT 0,
    avg_confidence DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (danger_behavior_id) REFERENCES danger_behaviors(id) ON DELETE CASCADE,
    UNIQUE KEY unique_stat (date, hour, danger_behavior_id),
    INDEX idx_date (date),
    INDEX idx_behavior (danger_behavior_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
