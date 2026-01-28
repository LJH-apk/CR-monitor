-- 训练样本表
CREATE TABLE training_samples (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    image_width INT,
    image_height INT,
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, ANNOTATED, APPROVED, REJECTED
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_upload_time (upload_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 样本标注表（支持多个标注框）
CREATE TABLE sample_annotations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sample_id BIGINT NOT NULL,
    danger_behavior_id BIGINT NOT NULL,
    x_min INT NOT NULL,
    y_min INT NOT NULL,
    x_max INT NOT NULL,
    y_max INT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (sample_id) REFERENCES training_samples(id) ON DELETE CASCADE,
    FOREIGN KEY (danger_behavior_id) REFERENCES danger_behaviors(id) ON DELETE CASCADE,
    INDEX idx_sample_id (sample_id),
    INDEX idx_behavior (danger_behavior_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
