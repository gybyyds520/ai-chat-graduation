-- ============================================================
-- V30: ai_character 表补充 avatar_style 列（预设头像风格持久化）
-- 幂等：仅当列不存在时执行 ADD COLUMN（MySQL 8 不支持 IF NOT EXISTS 加列）
-- ============================================================

SET @col_ai_character_avatar_style := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ai_character' AND COLUMN_NAME='avatar_style');
SET @ddl_ai_character_avatar_style := IF(@col_ai_character_avatar_style=0, "ALTER TABLE `ai_character` ADD COLUMN `avatar_style` varchar(30) NOT NULL DEFAULT 'avataaars' COMMENT '头像生成风格：avataaars/bottts/bottts-neutral/jdenticon/gridy/micah'", 'SELECT 1');
PREPARE s_ai_character_avatar_style FROM @ddl_ai_character_avatar_style;
EXECUTE s_ai_character_avatar_style;
DEALLOCATE PREPARE s_ai_character_avatar_style;
