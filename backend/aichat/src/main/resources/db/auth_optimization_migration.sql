ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `phone_verified` BOOLEAN DEFAULT FALSE COMMENT '手机号是否已验证';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `email_verified` BOOLEAN DEFAULT FALSE COMMENT '邮箱是否已验证';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `mfa_enabled` BOOLEAN DEFAULT FALSE COMMENT '是否启用多因素认证';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `mfa_secret` VARCHAR(100) DEFAULT NULL COMMENT '多因素认证密钥';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '上次登录IP';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `last_login_time` DATETIME DEFAULT NULL COMMENT '上次登录时间';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `account_status` VARCHAR(20) DEFAULT 'active' COMMENT '账号状态(active/disabled/locked)';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `failed_login_count` INT DEFAULT 0 COMMENT '连续失败登录次数';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `lock_time` DATETIME DEFAULT NULL COMMENT '账号锁定时间';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `security_question` VARCHAR(200) DEFAULT NULL COMMENT '安全问题';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `security_answer` VARCHAR(200) DEFAULT NULL COMMENT '安全答案';

CREATE TABLE IF NOT EXISTS `sms_code` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `code` VARCHAR(10) NOT NULL COMMENT '验证码',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `used` BOOLEAN DEFAULT FALSE COMMENT '是否已使用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_phone (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信验证码';

CREATE TABLE IF NOT EXISTS `login_device` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `device_id` VARCHAR(100) NOT NULL COMMENT '设备唯一标识',
    `device_name` VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
    `device_type` VARCHAR(50) DEFAULT NULL COMMENT '设备类型(pc/mobile/tablet)',
    `os` VARCHAR(100) DEFAULT NULL COMMENT '操作系统',
    `browser` VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '地理位置',
    `last_login_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间',
    `is_trusted` BOOLEAN DEFAULT FALSE COMMENT '是否为信任设备',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_device_id (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录设备管理';

CREATE TABLE IF NOT EXISTS `login_alert` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `alert_type` VARCHAR(50) NOT NULL COMMENT '告警类型(异地登录/异常登录/设备变更)',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '地理位置',
    `device_name` VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
    `login_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `status` VARCHAR(20) DEFAULT 'unread' COMMENT '状态(unread/read/handled)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录告警';

CREATE TABLE IF NOT EXISTS `user_badge` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `badge_id` VARCHAR(50) NOT NULL COMMENT '徽章ID',
    `badge_name` VARCHAR(100) NOT NULL COMMENT '徽章名称',
    `badge_icon` VARCHAR(200) DEFAULT NULL COMMENT '徽章图标',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '徽章描述',
    `earned_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成就徽章';

CREATE TABLE IF NOT EXISTS `user_tag` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `tag_category` VARCHAR(50) DEFAULT 'custom' COMMENT '标签分类(custom/system/interest)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户标签';

CREATE TABLE IF NOT EXISTS `oauth_provider` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `provider` VARCHAR(50) NOT NULL COMMENT '第三方平台(wechat/qq/github/google)',
    `provider_id` VARCHAR(100) NOT NULL COMMENT '第三方用户ID',
    `access_token` VARCHAR(500) DEFAULT NULL COMMENT '访问令牌',
    `refresh_token` VARCHAR(500) DEFAULT NULL COMMENT '刷新令牌',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`),
    UNIQUE INDEX uk_provider_user (`provider`, `provider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方登录绑定';

CREATE TABLE IF NOT EXISTS `notification_setting` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `notification_type` VARCHAR(50) NOT NULL COMMENT '通知类型(message/follow/comment/like/system)',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `push_enabled` BOOLEAN DEFAULT FALSE COMMENT '是否启用推送',
    `email_enabled` BOOLEAN DEFAULT FALSE COMMENT '是否启用邮件',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知设置';

CREATE TABLE IF NOT EXISTS `data_export_request` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `export_type` VARCHAR(50) NOT NULL COMMENT '导出类型(full/messages/profile/social)',
    `status` VARCHAR(20) DEFAULT 'processing' COMMENT '状态(processing/completed/failed)',
    `file_path` VARCHAR(500) DEFAULT NULL COMMENT '导出文件路径',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据导出请求';

DELETE FROM `sms_code` WHERE expire_time <= NOW();
DELETE FROM `login_device` WHERE last_login_time <= DATE_SUB(NOW(), INTERVAL 90 DAY);