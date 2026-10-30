-- =====================================================================
-- V7: User 表垂直拆分 - Phase 3 (P3-数据库优化) 引入
-- =====================================================================
-- 目的：将 user 单表（27 字段混合基本信息+安全+账户）拆分为 3 张表，
--       减少敏感字段暴露面、提升查询性能、便于权限隔离。
--
-- 拆分方案：
--   1. user                  - 基础信息（公开可见，列表查询）
--   2. user_security          - 安全字段（敏感，仅认证服务访问）
--   3. user_account           - 账户状态/订阅（管理后台访问）
--
-- 兼容性策略：
--   - 原 user 表保留全部列（向后兼容，避免破坏现有代码）；
--   - 新表通过 user_id 关联，由代码层做读写分离；
--   - 数据迁移脚本可重复执行（INSERT IGNORE / ON DUPLICATE KEY UPDATE）；
--   - 后续 P4 阶段可删除 user 表中的敏感列。
-- =====================================================================

-- 1. 创建 user_security 表（安全字段）
CREATE TABLE IF NOT EXISTS `user_security` (
    `user_id` BIGINT NOT NULL COMMENT '关联 user.id',
    `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码哈希',
    `mfa_enabled` TINYINT(1) DEFAULT 0 COMMENT '是否启用 MFA',
    `mfa_secret` VARCHAR(100) DEFAULT NULL COMMENT 'MFA 密钥（加密存储）',
    `failed_login_count` INT DEFAULT 0 COMMENT '登录失败次数',
    `lock_time` DATETIME DEFAULT NULL COMMENT '账户锁定时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最近登录 IP',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最近登录时间',
    `security_question` VARCHAR(200) DEFAULT NULL COMMENT '密保问题（加密存储）',
    `security_answer` VARCHAR(200) DEFAULT NULL COMMENT '密保答案（加密存储）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_user_security_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户安全信息表（敏感字段隔离）';

-- 2. 创建 user_account 表（账户状态与订阅）
CREATE TABLE IF NOT EXISTS `user_account` (
    `user_id` BIGINT NOT NULL COMMENT '关联 user.id',
    `version_type` VARCHAR(20) DEFAULT 'FREE' COMMENT '订阅类型（FREE/PREMIUM/ENTERPRISE）',
    `version_expire_time` DATETIME DEFAULT NULL COMMENT '订阅到期时间',
    `account_status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '账户状态（ACTIVE/LOCKED/DELETED/SUSPENDED）',
    `phone_verified` TINYINT(1) DEFAULT 0 COMMENT '手机是否验证',
    `email_verified` TINYINT(1) DEFAULT 0 COMMENT '邮箱是否验证',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_user_account_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_user_account_status` (`account_status`),
    INDEX `idx_user_account_version` (`version_type`, `version_expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账户状态表（订阅与状态）';

-- 3. 数据迁移：从 user 表复制已存在记录到新表（可重复执行）
INSERT IGNORE INTO `user_security` (`user_id`, `password_hash`, `mfa_enabled`, `mfa_secret`,
    `failed_login_count`, `lock_time`, `last_login_ip`, `last_login_time`,
    `security_question`, `security_answer`)
SELECT `id`, `password`, `mfa_enabled`, `mfa_secret`,
       `failed_login_count`, `lock_time`, `last_login_ip`, `last_login_time`,
       `security_question`, `security_answer`
FROM `user`
WHERE `password` IS NOT NULL OR `password` != '';

INSERT IGNORE INTO `user_account` (`user_id`, `version_type`, `version_expire_time`,
    `account_status`, `phone_verified`, `email_verified`)
SELECT `id`, `version_type`, `version_expire_time`,
       `account_status`,
       IF(`phone` IS NOT NULL AND `phone` != '', 1, 0),
       IF(`email` IS NOT NULL AND `email` != '', 1, 0)
FROM `user`;

-- 4. 在 user 表添加外键约束（可选，仅在数据完整性要求高时启用）
-- ALTER TABLE `user`
--     ADD INDEX `idx_user_id` (`id`),
--     ADD CONSTRAINT `fk_user_security` FOREIGN KEY (`id`)
--         REFERENCES `user_security` (`user_id`);

-- 5. 性能索引：新表关键查询
CREATE INDEX IF NOT EXISTS `idx_user_security_last_login` ON `user_security` (`last_login_time`);
CREATE INDEX IF NOT EXISTS `idx_user_account_status_version` ON `user_account` (`account_status`, `version_type`);
