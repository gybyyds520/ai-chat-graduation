-- ============================================================
-- V36: user 表新增"未成年风险标记"列 is_minor
-- 1. is_minor  未成年风险标记：由自声明年龄(age<18)或人工审核维护，
--              供内容审核/风控快速判定，默认 0（非未成年）。
-- 说明：幂等写法（information_schema 检查 + 动态 DDL），可重复执行。
--       既有 minor_mode_enabled（三态开关）保持不变，本列仅为显式风险标记。
-- ============================================================

SET @has_is_minor := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'is_minor');
SET @ddl_is_minor := IF(@has_is_minor = 0, 'ALTER TABLE `user` ADD COLUMN `is_minor` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''未成年风险标记：根据自声明年龄或人工审核标记，1=未成年''', 'SELECT 1');
PREPARE s_is_minor FROM @ddl_is_minor;
EXECUTE s_is_minor;
DEALLOCATE PREPARE s_is_minor;

-- 回填：已声明年龄 0<age<18 的存量用户，标记 is_minor=1（幂等：仅更新未标记的）
UPDATE `user` SET `is_minor` = 1 WHERE `is_minor` = 0 AND `age` IS NOT NULL AND `age` > 0 AND `age` < 18;
