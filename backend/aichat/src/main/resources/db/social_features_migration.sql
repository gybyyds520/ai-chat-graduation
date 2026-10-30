CREATE TABLE IF NOT EXISTS `social_post` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '发布者ID',
  `user_name` VARCHAR(50) COMMENT '发布者名称',
  `content` TEXT COMMENT '动态内容',
  `media_type` VARCHAR(20) DEFAULT 'text' COMMENT '媒体类型(text/image/video/mixed)',
  `media_urls` TEXT COMMENT '媒体URL列表(JSON数组)',
  `video_url` VARCHAR(500) COMMENT '视频URL',
  `video_cover` VARCHAR(500) COMMENT '视频封面',
  `location` VARCHAR(200) COMMENT '位置信息',
  `visibility` VARCHAR(20) DEFAULT 'public' COMMENT '可见范围(public/friends/private)',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT DEFAULT 0 COMMENT '评论数',
  `share_count` INT DEFAULT 0 COMMENT '分享数',
  `is_deleted` BOOLEAN DEFAULT FALSE COMMENT '是否删除',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_created_at` (`created_at`),
  INDEX `idx_visibility` (`visibility`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社交动态表';

CREATE TABLE IF NOT EXISTS `social_comment` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `user_id` BIGINT NOT NULL COMMENT '评论者ID',
  `user_name` VARCHAR(50) COMMENT '评论者名称',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID(回复)',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `is_deleted` BOOLEAN DEFAULT FALSE COMMENT '是否删除',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_post_id` (`post_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社交评论表';

CREATE TABLE IF NOT EXISTS `social_like` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `target_type` VARCHAR(20) NOT NULL COMMENT '目标类型(post/comment)',
  `target_id` BIGINT NOT NULL COMMENT '目标ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞者ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
  INDEX `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社交点赞表';

CREATE TABLE IF NOT EXISTS `social_share` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `user_id` BIGINT NOT NULL COMMENT '分享者ID',
  `user_name` VARCHAR(50) COMMENT '分享者名称',
  `share_text` TEXT COMMENT '分享文案',
  `share_type` VARCHAR(20) DEFAULT 'feed' COMMENT '分享类型(feed/private/group)',
  `target_id` BIGINT COMMENT '目标ID(好友或群)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_post_id` (`post_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社交分享表';

CREATE TABLE IF NOT EXISTS `notification` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
  `type` VARCHAR(20) NOT NULL COMMENT '通知类型(like/comment/share/friend_request/system)',
  `target_type` VARCHAR(20) COMMENT '目标类型(post/comment/user)',
  `target_id` BIGINT COMMENT '目标ID',
  `actor_id` BIGINT COMMENT '触发者ID',
  `actor_name` VARCHAR(50) COMMENT '触发者名称',
  `message` TEXT COMMENT '通知消息',
  `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读',
  `is_deleted` BOOLEAN DEFAULT FALSE COMMENT '是否删除',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_is_read` (`is_read`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

CREATE TABLE IF NOT EXISTS `friend_group` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分组名称',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_user_name` (`user_id`, `name`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友分组表';

CREATE TABLE IF NOT EXISTS `friend_group_member` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `group_id` BIGINT NOT NULL COMMENT '分组ID',
  `friend_id` BIGINT NOT NULL COMMENT '好友ID',
  `remark` VARCHAR(50) COMMENT '备注名称',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_group_friend` (`group_id`, `friend_id`),
  INDEX `idx_group_id` (`group_id`),
  INDEX `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友分组成员表';

CREATE TABLE IF NOT EXISTS `privacy_setting` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `key` VARCHAR(100) NOT NULL COMMENT '设置键',
  `value` TEXT COMMENT '设置值(JSON格式)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_user_key` (`user_id`, `key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='隐私设置表';

CREATE TABLE IF NOT EXISTS `user_follow` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `follower_id` BIGINT NOT NULL COMMENT '关注者ID',
  `following_id` BIGINT NOT NULL COMMENT '被关注者ID',
  `is_mutual` BOOLEAN DEFAULT FALSE COMMENT '是否互关',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_follow` (`follower_id`, `following_id`),
  INDEX `idx_follower` (`follower_id`),
  INDEX `idx_following` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

ALTER TABLE `friendship` 
  ADD COLUMN `remark` VARCHAR(50) COMMENT '好友备注' AFTER `status`,
  ADD COLUMN `group_id` BIGINT COMMENT '分组ID' AFTER `remark`,
  ADD INDEX `idx_group_id` (`group_id`);