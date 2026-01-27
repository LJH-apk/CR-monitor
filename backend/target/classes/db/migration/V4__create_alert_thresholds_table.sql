CREATE TABLE alert_thresholds (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    danger_behavior_id BIGINT NOT NULL,
    confidence_threshold DECIMAL(5,2) DEFAULT 0.70,
    time_window_seconds INT DEFAULT 60,
    max_alerts_per_window INT DEFAULT 10,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (danger_behavior_id) REFERENCES danger_behaviors(id) ON DELETE CASCADE,
    INDEX idx_behavior (danger_behavior_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default thresholds for each behavior
INSERT INTO alert_thresholds (danger_behavior_id, confidence_threshold, time_window_seconds, max_alerts_per_window, is_active)
SELECT id, 0.70, 60, 10, TRUE FROM danger_behaviors;
