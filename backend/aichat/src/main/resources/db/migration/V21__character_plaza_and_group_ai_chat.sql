-- V21: 角色广场/字段丰富化/多 AI 群聊 相关表结构
--
-- 1. ai_character 扩展：使用次数/评分/简介/分类/开场白/对话示例/标签/音色
-- 2. 新建 group_ai_member：群聊中的 AI 角色成员
-- 说明：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，改用 information_schema
--       检查 + 动态 DDL，保证脚本幂等可重复执行。

-- ai_character 广场与丰富化字段（幂等：列不存在时才 ADD）
SET @has_use_count := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_character' AND COLUMN_NAME = 'use_count');
SET @ddl_use_count := IF(@has_use_count = 0, 'ALTER TABLE ai_character ADD COLUMN use_count INT DEFAULT 0 COMMENT ''使用次数（开始对话时+1）''', 'SELECT 1');
PREPARE s_use_count FROM @ddl_use_count;
EXECUTE s_use_count;
DEALLOCATE PREPARE s_use_count;

SET @has_rating := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_character' AND COLUMN_NAME = 'rating');
SET @ddl_rating := IF(@has_rating = 0, 'ALTER TABLE ai_character ADD COLUMN rating DOUBLE DEFAULT 0 COMMENT ''平均评分 0-5''', 'SELECT 1');
PREPARE s_rating FROM @ddl_rating;
EXECUTE s_rating;
DEALLOCATE PREPARE s_rating;

SET @has_description := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_character' AND COLUMN_NAME = 'description');
SET @ddl_description := IF(@has_description = 0, 'ALTER TABLE ai_character ADD COLUMN description VARCHAR(512) NULL COMMENT ''角色一句话简介''', 'SELECT 1');
PREPARE s_description FROM @ddl_description;
EXECUTE s_description;
DEALLOCATE PREPARE s_description;

SET @has_category := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_character' AND COLUMN_NAME = 'category');
SET @ddl_category := IF(@has_category = 0, 'ALTER TABLE ai_character ADD COLUMN category VARCHAR(64) NULL COMMENT ''分类''', 'SELECT 1');
PREPARE s_category FROM @ddl_category;
EXECUTE s_category;
DEALLOCATE PREPARE s_category;

SET @has_greeting := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_character' AND COLUMN_NAME = 'greeting');
SET @ddl_greeting := IF(@has_greeting = 0, 'ALTER TABLE ai_character ADD COLUMN greeting VARCHAR(1024) NULL COMMENT ''开场白''', 'SELECT 1');
PREPARE s_greeting FROM @ddl_greeting;
EXECUTE s_greeting;
DEALLOCATE PREPARE s_greeting;

SET @has_example_dialogue := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_character' AND COLUMN_NAME = 'example_dialogue');
SET @ddl_example_dialogue := IF(@has_example_dialogue = 0, 'ALTER TABLE ai_character ADD COLUMN example_dialogue TEXT NULL COMMENT ''对话示例 few-shot''', 'SELECT 1');
PREPARE s_example_dialogue FROM @ddl_example_dialogue;
EXECUTE s_example_dialogue;
DEALLOCATE PREPARE s_example_dialogue;

SET @has_tags := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_character' AND COLUMN_NAME = 'tags');
SET @ddl_tags := IF(@has_tags = 0, 'ALTER TABLE ai_character ADD COLUMN tags VARCHAR(512) NULL COMMENT ''标签，逗号分隔''', 'SELECT 1');
PREPARE s_tags FROM @ddl_tags;
EXECUTE s_tags;
DEALLOCATE PREPARE s_tags;

SET @has_voice_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_character' AND COLUMN_NAME = 'voice_id');
SET @ddl_voice_id := IF(@has_voice_id = 0, 'ALTER TABLE ai_character ADD COLUMN voice_id VARCHAR(128) NULL COMMENT ''关联音色 ID''', 'SELECT 1');
PREPARE s_voice_id FROM @ddl_voice_id;
EXECUTE s_voice_id;
DEALLOCATE PREPARE s_voice_id;

-- 多 AI 群聊：群内 AI 角色成员表
CREATE TABLE IF NOT EXISTS group_ai_member (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_id BIGINT NOT NULL COMMENT '群 ID',
    character_id BIGINT NOT NULL COMMENT 'AI 角色 ID',
    added_by BIGINT NULL COMMENT '添加者 user id',
    created_at DATETIME NULL COMMENT '添加时间',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否激活',
    PRIMARY KEY (id),
    KEY idx_group_ai_group (group_id),
    KEY idx_group_ai_character (character_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊 AI 角色成员';
