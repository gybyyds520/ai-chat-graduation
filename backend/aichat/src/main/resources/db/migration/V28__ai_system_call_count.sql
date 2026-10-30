-- ============================================================
-- V28: ai_system 表补充 call_count 列（实体新增字段，统计调用次数）
-- 幂等：仅当列不存在时执行 ADD COLUMN（MySQL 8 不支持 IF NOT EXISTS 加列）
-- ============================================================

SET @col_ai_system_call_count := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ai_system' AND COLUMN_NAME='call_count');
SET @ddl_ai_system_call_count := IF(@col_ai_system_call_count=0, 'ALTER TABLE `ai_system` ADD COLUMN `call_count` int NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE s_ai_system_call_count FROM @ddl_ai_system_call_count;
EXECUTE s_ai_system_call_count;
DEALLOCATE PREPARE s_ai_system_call_count;
