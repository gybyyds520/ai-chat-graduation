ALTER TABLE `ai_elf` ADD COLUMN IF NOT EXISTS `focus_count` INT DEFAULT 0 COMMENT '专注次数';
ALTER TABLE `ai_elf` ADD COLUMN IF NOT EXISTS `total_focus_time` INT DEFAULT 0 COMMENT '总专注时长(分钟)';
ALTER TABLE `ai_elf` ADD COLUMN IF NOT EXISTS `health_points` INT DEFAULT 100 COMMENT '健康值';

ALTER TABLE `ai_elf_alarm` ADD COLUMN IF NOT EXISTS `smart_enabled` BOOLEAN DEFAULT FALSE COMMENT '是否智能闹钟';
ALTER TABLE `ai_elf_alarm` ADD COLUMN IF NOT EXISTS `weather_enabled` BOOLEAN DEFAULT FALSE COMMENT '是否天气提醒';
ALTER TABLE `ai_elf_alarm` ADD COLUMN IF NOT EXISTS `sleep_based` BOOLEAN DEFAULT FALSE COMMENT '是否基于睡眠';

ALTER TABLE `ai_elf_schedule` ADD COLUMN IF NOT EXISTS `priority` VARCHAR(20) DEFAULT 'normal' COMMENT '优先级(high/normal/low)';
ALTER TABLE `ai_elf_schedule` ADD COLUMN IF NOT EXISTS `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间';
ALTER TABLE `ai_elf_schedule` ADD COLUMN IF NOT EXISTS `recurrence` VARCHAR(50) DEFAULT NULL COMMENT '重复规则';

CREATE TABLE IF NOT EXISTS `ai_elf_focus` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `elf_id` BIGINT NOT NULL COMMENT '小精灵ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `duration_minutes` INT DEFAULT 0 COMMENT '专注时长(分钟)',
    `goal` VARCHAR(200) DEFAULT NULL COMMENT '专注目标',
    `completed` BOOLEAN DEFAULT FALSE COMMENT '是否完成',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (`elf_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专注记录';

CREATE TABLE IF NOT EXISTS `ai_elf_health_reminder` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `elf_id` BIGINT NOT NULL COMMENT '小精灵ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `reminder_type` VARCHAR(50) NOT NULL COMMENT '提醒类型(drink/walk/eye/rest)',
    `last_reminded` DATETIME DEFAULT NULL COMMENT '上次提醒时间',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `interval_minutes` INT DEFAULT 60 COMMENT '提醒间隔(分钟)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (`elf_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康提醒';

CREATE TABLE IF NOT EXISTS `sleep_analysis_report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `elf_id` BIGINT NOT NULL COMMENT '小精灵ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `report_date` DATE NOT NULL COMMENT '报告日期',
    `avg_duration` DOUBLE DEFAULT 0 COMMENT '平均睡眠时长',
    `avg_quality` INT DEFAULT 0 COMMENT '平均睡眠质量',
    `sleep_score` INT DEFAULT 0 COMMENT '睡眠评分',
    `suggestions` TEXT DEFAULT NULL COMMENT '改善建议',
    `deep_sleep_ratio` DOUBLE DEFAULT 0 COMMENT '深度睡眠比例',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (`elf_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='睡眠分析报告';

CREATE TABLE IF NOT EXISTS `ai_elf_outfit` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `elf_id` BIGINT NOT NULL COMMENT '小精灵ID',
    `outfit_name` VARCHAR(100) NOT NULL COMMENT '服装名称',
    `outfit_type` VARCHAR(50) DEFAULT 'clothes' COMMENT '服装类型(clothes/accessory/emoji)',
    `equipped` BOOLEAN DEFAULT FALSE COMMENT '是否已装备',
    `unlocked` BOOLEAN DEFAULT TRUE COMMENT '是否已解锁',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (`elf_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小精灵换装';