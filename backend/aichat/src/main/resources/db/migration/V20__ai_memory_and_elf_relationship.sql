-- ============================================================
-- V20: AI 记忆可视化 + AI 精灵养成深化
-- 1. 新建 ai_memory 表（长期记忆实体）
-- 2. ai_elf 表扩展：好感度/关系等级/亲密度/最后互动时间（MySQL 8.0 幂等）
-- 3. 新建 ai_elf_interaction_log 表（精灵互动日志）
-- 说明：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，改用 information_schema
--       检查 + 动态 DDL，保证脚本可重复执行且幂等。
-- ============================================================

-- 1) AI 长期记忆表
CREATE TABLE IF NOT EXISTS `ai_memory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `content` TEXT COMMENT '记忆内容',
    `source` VARCHAR(16) NOT NULL DEFAULT 'manual' COMMENT '来源: chat/manual/profile/system',
    `memory_type` VARCHAR(16) NOT NULL DEFAULT 'other' COMMENT '类型: fact/preference/event/personality/other',
    `importance` INT NOT NULL DEFAULT 3 COMMENT '重要性 1-5',
    `category` VARCHAR(64) DEFAULT NULL COMMENT '分类(可空)',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否生效',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_used_at` DATETIME DEFAULT NULL COMMENT '最近注入上下文时间',
    PRIMARY KEY (`id`),
    KEY `idx_memory_user_id` (`user_id`),
    KEY `idx_memory_type` (`memory_type`),
    KEY `idx_memory_active` (`is_active`),
    KEY `idx_memory_importance` (`importance`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 长期记忆表';

-- 2) ai_elf 养成字段扩展（幂等：列不存在时才 ADD）
SET @has_affection := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf' AND COLUMN_NAME = 'affection');
SET @ddl_affection := IF(@has_affection = 0, 'ALTER TABLE `ai_elf` ADD COLUMN `affection` INT NOT NULL DEFAULT 0 COMMENT ''好感度 0-100''', 'SELECT 1');
PREPARE s_affection FROM @ddl_affection;
EXECUTE s_affection;
DEALLOCATE PREPARE s_affection;

SET @has_relationship := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf' AND COLUMN_NAME = 'relationship_level');
SET @ddl_relationship := IF(@has_relationship = 0, 'ALTER TABLE `ai_elf` ADD COLUMN `relationship_level` INT NOT NULL DEFAULT 1 COMMENT ''关系等级 1-10''', 'SELECT 1');
PREPARE s_relationship FROM @ddl_relationship;
EXECUTE s_relationship;
DEALLOCATE PREPARE s_relationship;

SET @has_intimacy := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf' AND COLUMN_NAME = 'intimacy');
SET @ddl_intimacy := IF(@has_intimacy = 0, 'ALTER TABLE `ai_elf` ADD COLUMN `intimacy` INT NOT NULL DEFAULT 0 COMMENT ''亲密度 0-1000''', 'SELECT 1');
PREPARE s_intimacy FROM @ddl_intimacy;
EXECUTE s_intimacy;
DEALLOCATE PREPARE s_intimacy;

SET @has_last_interact := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf' AND COLUMN_NAME = 'last_interact_at');
SET @ddl_last_interact := IF(@has_last_interact = 0, 'ALTER TABLE `ai_elf` ADD COLUMN `last_interact_at` DATETIME DEFAULT NULL COMMENT ''最后互动时间''', 'SELECT 1');
PREPARE s_last_interact FROM @ddl_last_interact;
EXECUTE s_last_interact;
DEALLOCATE PREPARE s_last_interact;

-- 3) 精灵互动日志表
CREATE TABLE IF NOT EXISTS `ai_elf_interaction_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `elf_id` BIGINT NOT NULL COMMENT '精灵ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `interaction_type` VARCHAR(16) NOT NULL COMMENT '互动类型: chat/feed/play/sleep',
    `points_gained` INT NOT NULL DEFAULT 0 COMMENT '本次获得好感度',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_elf_log_elf_id` (`elf_id`),
    KEY `idx_elf_log_user_id` (`user_id`),
    KEY `idx_elf_log_type` (`interaction_type`),
    KEY `idx_elf_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 精灵互动日志表';
