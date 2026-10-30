-- 创建角色分享表
CREATE TABLE IF NOT EXISTS `character_share` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `character_id` BIGINT NOT NULL COMMENT '角色ID',
  `shared_by_user_id` BIGINT NOT NULL COMMENT '分享者用户ID',
  `shared_to_user_id` BIGINT NOT NULL COMMENT '被分享者用户ID',
  `permissions` VARCHAR(100) DEFAULT 'read' COMMENT '权限(read/chat/manage)',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/REVOKED)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_character_recipient` (`character_id`, `shared_to_user_id`),
  INDEX `idx_character_id` (`character_id`),
  INDEX `idx_shared_to_user_id` (`shared_to_user_id`),
  INDEX `idx_shared_by_user_id` (`shared_by_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色分享关系表';

-- 创建角色偏好设置表
CREATE TABLE IF NOT EXISTS `character_preference` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `character_id` BIGINT NOT NULL COMMENT '角色ID',
  `preferences` TEXT COMMENT '偏好设置(JSON格式)',
  `last_used_at` DATETIME COMMENT '最后使用时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_user_character` (`user_id`, `character_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_character_id` (`character_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色用户偏好设置表';

-- 创建角色聊天记录关联表（用于记录用户与角色的交互）
CREATE TABLE IF NOT EXISTS `character_interaction` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `character_id` BIGINT NOT NULL COMMENT '角色ID',
  `interaction_count` INT DEFAULT 0 COMMENT '交互次数',
  `total_messages` INT DEFAULT 0 COMMENT '总消息数',
  `first_interaction_at` DATETIME COMMENT '首次交互时间',
  `last_interaction_at` DATETIME COMMENT '最后交互时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_user_character_interaction` (`user_id`, `character_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_character_id` (`character_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色用户交互记录表';

-- 更新ai_character表，确保is_system和is_shared有默认值
ALTER TABLE `ai_character` MODIFY COLUMN `is_system` BOOLEAN DEFAULT FALSE COMMENT '是否系统角色';
ALTER TABLE `ai_character` MODIFY COLUMN `is_shared` BOOLEAN DEFAULT FALSE COMMENT '是否全局分享';

-- 添加外键约束（可选，根据实际需求决定是否启用）
-- ALTER TABLE `character_share` ADD CONSTRAINT `fk_share_character` FOREIGN KEY (`character_id`) REFERENCES `ai_character`(`id`) ON DELETE CASCADE;
-- ALTER TABLE `character_share` ADD CONSTRAINT `fk_share_by_user` FOREIGN KEY (`shared_by_user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE;
-- ALTER TABLE `character_share` ADD CONSTRAINT `fk_share_to_user` FOREIGN KEY (`shared_to_user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE;
-- ALTER TABLE `character_preference` ADD CONSTRAINT `fk_pref_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE;
-- ALTER TABLE `character_preference` ADD CONSTRAINT `fk_pref_character` FOREIGN KEY (`character_id`) REFERENCES `ai_character`(`id`) ON DELETE CASCADE;