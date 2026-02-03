-- 创建系统日志表
CREATE TABLE system_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 日志级别: DEBUG, INFO, WARN, ERROR
    level VARCHAR(10) NOT NULL DEFAULT 'INFO',

    -- 日志类型: LOGIN, LOGOUT, OPERATION, ERROR, SYSTEM
    type VARCHAR(20) NOT NULL,

    -- 日志消息
    message VARCHAR(500) NOT NULL,

    -- 详细信息（错误堆栈、请求参数等）
    details TEXT,

    -- 关联用户（可为空，系统日志无用户）
    user_id BIGINT,
    username VARCHAR(50),

    -- 请求信息
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_uri VARCHAR(255),
    request_method VARCHAR(10),

    -- 地理位置（通过 IP 解析）
    location VARCHAR(100),

    -- 登录相关
    login_success BOOLEAN,

    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 索引
    INDEX idx_level (level),
    INDEX idx_type (type),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_level_type (level, type),

    -- 外键（允许用户删除后日志保留）
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入开发者测试用户（密码: admin123）
INSERT INTO users (username, password, email, role) VALUES
('developer', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'developer@example.com', 'DEVELOPER');
