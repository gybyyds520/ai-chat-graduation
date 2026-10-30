CREATE TABLE IF NOT EXISTS `user_interest` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `interest` VARCHAR(100) NOT NULL COMMENT '兴趣关键词',
    `weight` INT DEFAULT 1 COMMENT '权重',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_interest (`interest`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户兴趣表';

CREATE TABLE IF NOT EXISTS `user_view_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `target_type` VARCHAR(50) NOT NULL COMMENT '浏览类型(post/user)',
    `target_id` BIGINT NOT NULL COMMENT '浏览对象ID',
    `view_count` INT DEFAULT 1 COMMENT '浏览次数',
    `last_view_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后浏览时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_target (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户浏览记录表';

CREATE TABLE IF NOT EXISTS `user_block` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `blocked_user_id` BIGINT NOT NULL COMMENT '被拉黑用户ID',
    `blocked_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '拉黑时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_blocked_user_id (`blocked_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户拉黑表';

CREATE TABLE IF NOT EXISTS `user_report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '举报用户ID',
    `target_type` VARCHAR(50) NOT NULL COMMENT '举报类型(post/comment/user)',
    `target_id` BIGINT NOT NULL COMMENT '举报对象ID',
    `report_reason` VARCHAR(500) NOT NULL COMMENT '举报原因',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/REVIEWED/RESOLVED)',
    `review_result` VARCHAR(500) COMMENT '审核结果',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `reviewed_at` DATETIME COMMENT '审核时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_target (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户举报表';

CREATE TABLE IF NOT EXISTS `social_post_tag` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `post_id` BIGINT NOT NULL COMMENT '动态ID',
    `tag_name` VARCHAR(100) NOT NULL COMMENT '标签名称',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_post_id (`post_id`),
    INDEX idx_tag_name (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态标签表';

CREATE TABLE IF NOT EXISTS `user_statistics` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `post_count` INT DEFAULT 0 COMMENT '帖子数量',
    `comment_count` INT DEFAULT 0 COMMENT '评论数量',
    `like_count` INT DEFAULT 0 COMMENT '获得点赞数量',
    `follower_count` INT DEFAULT 0 COMMENT '粉丝数量',
    `following_count` INT DEFAULT 0 COMMENT '关注数量',
    `share_count` INT DEFAULT 0 COMMENT '分享数量',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户统计表';

ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL' AFTER `password`;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `bio` TEXT DEFAULT NULL COMMENT '个人简介' AFTER `email`;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `interests` VARCHAR(500) DEFAULT NULL COMMENT '兴趣标签(逗号分隔)' AFTER `bio`;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `location` VARCHAR(200) DEFAULT NULL COMMENT '所在地' AFTER `interests`;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `website` VARCHAR(500) DEFAULT NULL COMMENT '个人网站' AFTER `location`;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别' AFTER `website`;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `birthday` DATE DEFAULT NULL COMMENT '生日' AFTER `gender`;