-- ============================================================
-- V26: 数据池配置表 + user_data_record 自动采集索引
-- 1. data_pool_config：键值对持久化 autoSave/retentionDays/maxRecordsPerUser
-- 2. user_data_record：补充 userId+dataType、capture_time、source_id 相关索引
-- 3. 写入默认配置：autoSaveEnabled=true, retentionDays=180, maxRecordsPerUser=10000
-- 幂等：表/索引已存在则跳过；配置主键冲突则忽略。
-- ============================================================

-- 1. 数据池配置表
CREATE TABLE IF NOT EXISTS `data_pool_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(500) DEFAULT NULL COMMENT '配置值',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据池配置表';

-- 2. user_data_record 索引（幂等判断，已存在则跳过）
SET @idx_user_type_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='user_data_record' AND INDEX_NAME='idx_user_id_data_type');
SET @ddl_idx_user_type := IF(@idx_user_type_exists=0,
    'CREATE INDEX `idx_user_id_data_type` ON `user_data_record` (`user_id`, `data_type`)', 'SELECT 1');
PREPARE s_idx_user_type FROM @ddl_idx_user_type;
EXECUTE s_idx_user_type;
DEALLOCATE PREPARE s_idx_user_type;

SET @idx_capture_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='user_data_record' AND INDEX_NAME='idx_capture_time');
SET @ddl_idx_capture := IF(@idx_capture_exists=0,
    'CREATE INDEX `idx_capture_time` ON `user_data_record` (`capture_time`)', 'SELECT 1');
PREPARE s_idx_capture FROM @ddl_idx_capture;
EXECUTE s_idx_capture;
DEALLOCATE PREPARE s_idx_capture;

-- sourceId 存于 metadata，这里对 metadata 建普通索引（MySQL 不支持函数索引时退化为普通列索引）
SET @idx_source_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='user_data_record' AND INDEX_NAME='idx_source_id');
SET @ddl_idx_source := IF(@idx_source_exists=0,
    'CREATE INDEX `idx_source_id` ON `user_data_record` (`group_id`)', 'SELECT 1');
PREPARE s_idx_source FROM @ddl_idx_source;
EXECUTE s_idx_source;
DEALLOCATE PREPARE s_idx_source;

-- 3. 默认配置（存在则忽略）
INSERT IGNORE INTO `data_pool_config` (`config_key`, `config_value`, `updated_at`) VALUES
    ('autoSaveEnabled', 'true', NOW()),
    ('retentionDays', '180', NOW()),
    ('maxRecordsPerUser', '10000', NOW());
