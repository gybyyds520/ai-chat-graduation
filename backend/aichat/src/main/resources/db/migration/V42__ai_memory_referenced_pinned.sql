-- ============================================================
-- V40: AI 记忆市场级升级 —— 衰减与优先级字段
-- 1. ai_memory 新增 referenced_count（引用次数，默认 0）
-- 2. ai_memory 新增 is_pinned（钉选，默认 0，钉选记忆不衰减）
-- 说明：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，沿用 information_schema 幂等写法。
--       last_used_at 语义即 lastReferencedAt（最后引用时间），复用既有列不重复建。
-- ============================================================

SET @has_refcnt := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_memory' AND COLUMN_NAME = 'referenced_count');
SET @ddl_refcnt := IF(@has_refcnt = 0,
    'ALTER TABLE `ai_memory` ADD COLUMN `referenced_count` INT NOT NULL DEFAULT 0 COMMENT ''被对话引用次数(>5加权)''',
    'SELECT 1');
PREPARE s_refcnt FROM @ddl_refcnt;
EXECUTE s_refcnt;
DEALLOCATE PREPARE s_refcnt;

SET @has_pinned := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_memory' AND COLUMN_NAME = 'is_pinned');
SET @ddl_pinned := IF(@has_pinned = 0,
    'ALTER TABLE `ai_memory` ADD COLUMN `is_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否钉选(不衰减)''',
    'SELECT 1');
PREPARE s_pinned FROM @ddl_pinned;
EXECUTE s_pinned;
DEALLOCATE PREPARE s_pinned;
