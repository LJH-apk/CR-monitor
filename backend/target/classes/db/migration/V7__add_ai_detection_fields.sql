-- 为alerts表添加AI检测相关字段
ALTER TABLE alerts
ADD COLUMN detection_bbox VARCHAR(100) COMMENT '检测框坐标 [x1,y1,x2,y2]',
ADD COLUMN analysis_text TEXT COMMENT '千问分析原文',
ADD COLUMN alert_category VARCHAR(50) COMMENT '预警类别',
ADD COLUMN alert_keyword VARCHAR(50) COMMENT '触发关键词';

-- 为danger_behaviors表添加关键词字段
ALTER TABLE danger_behaviors
ADD COLUMN keywords TEXT COMMENT '关联关键词（JSON数组）';

-- 更新现有危险行为的关键词
UPDATE danger_behaviors SET keywords = '["打架","斗殴"]' WHERE name = 'Fighting';
UPDATE danger_behaviors SET keywords = '["摔倒","晕倒"]' WHERE name = 'Falling';
UPDATE danger_behaviors SET keywords = '["非法进入","翻越"]' WHERE name = 'Intrusion';
UPDATE danger_behaviors SET keywords = '["吸烟"]' WHERE name = 'Smoking';
UPDATE danger_behaviors SET keywords = '["烟火","明火"]' WHERE name = 'Fire';
