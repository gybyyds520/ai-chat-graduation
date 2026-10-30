-- 为 ai_character 表添加 model 字段
ALTER TABLE ai_character ADD COLUMN model VARCHAR(100) DEFAULT 'phi3:mini' COMMENT '模型名称';

-- 为 ai_character 表添加用户隔离字段
ALTER TABLE ai_character ADD COLUMN user_id BIGINT DEFAULT NULL COMMENT '用户ID';
ALTER TABLE ai_character ADD COLUMN is_shared BOOLEAN DEFAULT FALSE COMMENT '是否分享';
ALTER TABLE ai_character ADD INDEX idx_user_id (user_id);

-- 为 friend_chat_message 表添加 type 字段
ALTER TABLE friend_chat_message ADD COLUMN type VARCHAR(50) DEFAULT 'text' COMMENT '消息类型(text/image/file/audio/video)';

-- 为 user 表添加 email 和 bio 字段
ALTER TABLE `user` ADD COLUMN email VARCHAR(255) DEFAULT NULL COMMENT '邮箱';
ALTER TABLE `user` ADD COLUMN bio TEXT DEFAULT NULL COMMENT '个人简介';

-- 创建用户数据记录表
CREATE TABLE IF NOT EXISTS `user_data_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `data_type` VARCHAR(50) NOT NULL COMMENT '数据类型(form/chat_message/user_action/editor_content)',
  `content` TEXT COMMENT '数据内容',
  `metadata` TEXT COMMENT '元数据(JSON格式)',
  `tags` VARCHAR(500) COMMENT '标签(逗号分隔)',
  `integrity_score` DOUBLE DEFAULT 0.5 COMMENT '完整性评分(0-1)',
  `status` VARCHAR(20) DEFAULT 'PENDING_VALIDATION' COMMENT '状态(PENDING_VALIDATION/VALIDATED/INVALID)',
  `capture_time` DATETIME COMMENT '捕获时间',
  `group_id` VARCHAR(100) COMMENT '分组ID',
  `backup_status` VARCHAR(20) DEFAULT 'PENDING_BACKUP' COMMENT '备份状态(PENDING_BACKUP/BACKED_UP/FAILED)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_user_id (`user_id`),
  INDEX idx_data_type (`data_type`),
  INDEX idx_capture_time (`capture_time`),
  INDEX idx_group_id (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户数据记录表';