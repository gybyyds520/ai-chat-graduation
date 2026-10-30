-- ============================================================
-- V23: user 表补充第三/五轮新增字段
-- 1. age              自声明年龄（未成年保护）
-- 2. minor_mode_enabled     未成年保护模式三态开关（NULL=按年龄自动）
-- 3. minor_mode_updated_at  未成年模式最近手动调整时间
-- 4. voice_preference       多音色 TTS 偏好（VoiceProfile 枚举）
-- 说明：幂等写法（information_schema 检查 + 动态 DDL），可重复执行。
-- ============================================================

SET @has_age := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'age');
SET @ddl_age := IF(@has_age = 0, 'ALTER TABLE `user` ADD COLUMN `age` INT NULL COMMENT ''自声明年龄，0或NULL表示未设置''', 'SELECT 1');
PREPARE s_age FROM @ddl_age;
EXECUTE s_age;
DEALLOCATE PREPARE s_age;

SET @has_minor_enabled := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'minor_mode_enabled');
SET @ddl_minor_enabled := IF(@has_minor_enabled = 0, 'ALTER TABLE `user` ADD COLUMN `minor_mode_enabled` TINYINT(1) NULL COMMENT ''未成年保护模式：TRUE=强制开启 FALSE=强制关闭 NULL=按年龄自动''', 'SELECT 1');
PREPARE s_minor_enabled FROM @ddl_minor_enabled;
EXECUTE s_minor_enabled;
DEALLOCATE PREPARE s_minor_enabled;

SET @has_minor_updated := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'minor_mode_updated_at');
SET @ddl_minor_updated := IF(@has_minor_updated = 0, 'ALTER TABLE `user` ADD COLUMN `minor_mode_updated_at` DATETIME NULL COMMENT ''未成年模式最近手动调整时间''', 'SELECT 1');
PREPARE s_minor_updated FROM @ddl_minor_updated;
EXECUTE s_minor_updated;
DEALLOCATE PREPARE s_minor_updated;

SET @has_voice := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'voice_preference');
SET @ddl_voice := IF(@has_voice = 0, 'ALTER TABLE `user` ADD COLUMN `voice_preference` VARCHAR(20) NULL DEFAULT ''default'' COMMENT ''多音色TTS偏好: default/warm/cold/young/old/female/male''', 'SELECT 1');
PREPARE s_voice FROM @ddl_voice;
EXECUTE s_voice;
DEALLOCATE PREPARE s_voice;
