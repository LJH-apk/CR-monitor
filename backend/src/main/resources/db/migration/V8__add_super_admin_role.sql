-- 添加SUPER_ADMIN角色支持
-- 更新现有admin用户为SUPER_ADMIN
UPDATE users SET role = 'SUPER_ADMIN' WHERE username = 'admin';

-- 插入测试用户
INSERT INTO users (username, password, email, role) VALUES
('user1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user1@example.com', 'USER'),
('admin1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin1@example.com', 'ADMIN');

-- 密码均为: admin123
