-- ============================================================
-- V38: AI 记忆全局开关表（M1 / A3）
-- 每用户一行：enabled 控制是否把该用户的长期记忆注入对话上下文。
-- 未设置记录时由服务层默认视为开启（enabled=1）。
-- MySQL 8.0 幂等建表。
-- ============================================================

CREATE TABLE IF NOT EXISTS `ai_memory_setting` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID（主键）',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '记忆总开关: 1=注入 0=不注入',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 记忆全局开关表';
