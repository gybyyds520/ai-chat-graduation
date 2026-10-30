-- 扩展好友聊天消息表
ALTER TABLE `friend_chat_message` 
  ADD COLUMN `message_type` VARCHAR(20) DEFAULT 'text' COMMENT '消息类型(text/emoji/file/system)' AFTER `content`,
  ADD COLUMN `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读' AFTER `type`,
  ADD COLUMN `read_at` DATETIME COMMENT '已读时间' AFTER `is_read`,
  ADD COLUMN `extra_data` TEXT COMMENT '扩展数据(JSON格式，用于表情等)' AFTER `fileSize`,
  ADD COLUMN `sender_name` VARCHAR(50) COMMENT '发送者名称(冗余字段)' AFTER `senderId`,
  ADD COLUMN `receiver_name` VARCHAR(50) COMMENT '接收者名称(冗余字段)' AFTER `receiverId`,
  ADD INDEX `idx_sender_read` (`senderId`, `is_read`),
  ADD INDEX `idx_receiver_read` (`receiverId`, `is_read`),
  ADD INDEX `idx_create_time` (`create_time`);

-- 创建用户在线状态表
CREATE TABLE IF NOT EXISTS `user_online_status` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `status` VARCHAR(20) DEFAULT 'offline' COMMENT '在线状态(online/offline/away/busy/invisible)',
  `last_active_time` DATETIME COMMENT '最后活跃时间',
  `session_id` VARCHAR(100) COMMENT 'WebSocket会话ID',
  `ip_address` VARCHAR(50) COMMENT 'IP地址',
  `device_info` VARCHAR(200) COMMENT '设备信息',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_user_id` (`user_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户在线状态表';

-- 创建聊天会话表
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `session_type` VARCHAR(20) NOT NULL COMMENT '会话类型(private/group)',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '目标ID(好友ID或群ID)',
  `target_name` VARCHAR(100) COMMENT '目标名称',
  `target_avatar` VARCHAR(255) COMMENT '目标头像',
  `last_message` TEXT COMMENT '最后一条消息',
  `last_message_time` DATETIME COMMENT '最后消息时间',
  `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
  `is_muted` BOOLEAN DEFAULT FALSE COMMENT '是否静音',
  `is_top` BOOLEAN DEFAULT FALSE COMMENT '是否置顶',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_user_target` (`user_id`, `session_type`, `target_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- 创建群聊相关表
CREATE TABLE IF NOT EXISTS `chat_group` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '群名称',
  `description` VARCHAR(500) COMMENT '群描述',
  `avatar` VARCHAR(255) COMMENT '群头像',
  `owner_id` BIGINT NOT NULL COMMENT '群主ID',
  `max_members` INT DEFAULT 200 COMMENT '最大成员数',
  `is_public` BOOLEAN DEFAULT TRUE COMMENT '是否公开群',
  `join_mode` VARCHAR(20) DEFAULT 'free' COMMENT '加入方式(free/approval/invite)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊表';

CREATE TABLE IF NOT EXISTS `group_member` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `group_id` BIGINT NOT NULL COMMENT '群ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `user_name` VARCHAR(50) COMMENT '用户昵称',
  `role` VARCHAR(20) DEFAULT 'member' COMMENT '角色(owner/admin/member)',
  `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `is_muted` BOOLEAN DEFAULT FALSE COMMENT '是否被禁言',
  `is_leave` BOOLEAN DEFAULT FALSE COMMENT '是否已退出',
  `leave_time` DATETIME COMMENT '退出时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
  INDEX `idx_group_id` (`group_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

CREATE TABLE IF NOT EXISTS `group_message` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `group_id` BIGINT NOT NULL COMMENT '群ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
  `sender_name` VARCHAR(50) COMMENT '发送者名称',
  `content` TEXT COMMENT '消息内容',
  `message_type` VARCHAR(20) DEFAULT 'text' COMMENT '消息类型(text/emoji/file/system)',
  `file_name` VARCHAR(255) COMMENT '文件名',
  `file_path` VARCHAR(500) COMMENT '文件路径',
  `file_size` BIGINT COMMENT '文件大小',
  `extra_data` TEXT COMMENT '扩展数据(JSON格式)',
  `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_group_id` (`group_id`),
  INDEX `idx_sender_id` (`sender_id`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息表';

-- 创建打字状态表
CREATE TABLE IF NOT EXISTS `typing_status` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '目标ID(好友ID或群ID)',
  `target_type` VARCHAR(20) DEFAULT 'private' COMMENT '目标类型(private/group)',
  `is_typing` BOOLEAN DEFAULT FALSE COMMENT '是否正在打字',
  `last_typing_time` DATETIME COMMENT '最后打字时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打字状态表';