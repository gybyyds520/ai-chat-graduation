-- ============================================================
-- V40: AI 精灵桌面养成系统升级
-- ai_elf 表扩展：心情值 mood / 饱腹度 fullness（0-100）
-- 说明：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，沿用 information_schema
--       检查 + 动态 DDL，保证脚本可重复执行且幂等。
-- 亲密度 intimacy 字段已在 V20 引入（0-1000），本迁移不再重复添加。
-- ============================================================

-- 心情值 mood：默认 80（开心），0-100
SET @has_mood := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf' AND COLUMN_NAME = 'mood');
SET @ddl_mood := IF(@has_mood = 0, 'ALTER TABLE `ai_elf` ADD COLUMN `mood` INT NOT NULL DEFAULT 80 COMMENT ''心情值 0-100''', 'SELECT 1');
PREPARE s_mood FROM @ddl_mood;
EXECUTE s_mood;
DEALLOCATE PREPARE s_mood;

-- 饱腹度 fullness：默认 70，0-100
SET @has_fullness := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf' AND COLUMN_NAME = 'fullness');
SET @ddl_fullness := IF(@has_fullness = 0, 'ALTER TABLE `ai_elf` ADD COLUMN `fullness` INT NOT NULL DEFAULT 70 COMMENT ''饱腹度 0-100''', 'SELECT 1');
PREPARE s_fullness FROM @ddl_fullness;
EXECUTE s_fullness;
DEALLOCATE PREPARE s_fullness;
