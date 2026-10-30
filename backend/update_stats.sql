-- 为 user 表添加 create_time 列
ALTER TABLE user ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 为 ai_character 表添加 create_time 列
ALTER TABLE ai_character ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 为已存在的记录设置 create_time（如果是今天之前创建的，可以设置为当前时间或特定时间）
UPDATE user SET create_time = NOW() WHERE create_time IS NULL;
UPDATE ai_character SET create_time = NOW() WHERE create_time IS NULL;
