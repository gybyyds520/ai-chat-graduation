CREATE TABLE IF NOT EXISTS `group_chat` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '群名称',
  `description` VARCHAR(500) COMMENT '群描述',
  `avatar` VARCHAR(255) COMMENT '群头像',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` INT DEFAULT 1 COMMENT '状态(0-禁用, 1-启用)',
  INDEX `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊表';

CREATE TABLE IF NOT EXISTS `group_chat_member` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `group_id` BIGINT NOT NULL COMMENT '群ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `nickname` VARCHAR(50) COMMENT '用户昵称',
  `role` VARCHAR(20) DEFAULT 'member' COMMENT '角色(admin/member)',
  `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `mute_until` DATETIME COMMENT '禁言到期时间',
  `status` INT DEFAULT 1 COMMENT '状态(0-退出, 1-正常)',
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
  INDEX `idx_group_id` (`group_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

CREATE TABLE IF NOT EXISTS `group_chat_message` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `group_id` BIGINT NOT NULL COMMENT '群ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
  `sender_name` VARCHAR(50) COMMENT '发送者名称',
  `content` TEXT COMMENT '消息内容',
  `message_type` VARCHAR(20) DEFAULT 'text' COMMENT '消息类型(text/emoji/file)',
  `file_name` VARCHAR(255) COMMENT '文件名',
  `file_path` VARCHAR(500) COMMENT '文件路径',
  `file_size` BIGINT COMMENT '文件大小',
  `extra_data` TEXT COMMENT '扩展数据(JSON格式)',
  `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `status` INT DEFAULT 0 COMMENT '状态(0-未读, 1-已读)',
  INDEX `idx_group_id` (`group_id`),
  INDEX `idx_sender_id` (`sender_id`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息表';