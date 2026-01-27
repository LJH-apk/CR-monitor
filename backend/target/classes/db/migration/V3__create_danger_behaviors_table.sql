CREATE TABLE danger_behaviors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    severity_level INT NOT NULL,
    color_code VARCHAR(7),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_severity (severity_level),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default danger behaviors
INSERT INTO danger_behaviors (name, description, severity_level, color_code, is_active) VALUES
('Fighting', 'Physical altercation detected', 4, '#FF0000', TRUE),
('Falling', 'Person falling or lying on ground', 3, '#FF6600', TRUE),
('Intrusion', 'Unauthorized entry detected', 3, '#FF9900', TRUE),
('Loitering', 'Suspicious loitering behavior', 2, '#FFCC00', TRUE),
('Running', 'Running in restricted area', 2, '#FFFF00', TRUE);
