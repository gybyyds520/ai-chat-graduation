CREATE TABLE IF NOT EXISTS `group_chat` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '群聊名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '群聊描述',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '群聊头像',
    `owner_id` BIGINT NOT NULL COMMENT '群主ID',
    `member_count` INT DEFAULT 1 COMMENT '成员数量',
    `is_public` BOOLEAN DEFAULT TRUE COMMENT '是否公开',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_owner_id (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊';

CREATE TABLE IF NOT EXISTS `group_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `group_id` BIGINT NOT NULL COMMENT '群聊ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(50) DEFAULT 'member' COMMENT '角色(owner/admin/member)',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    INDEX idx_group_id (`group_id`),
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员';

CREATE TABLE IF NOT EXISTS `group_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `group_id` BIGINT NOT NULL COMMENT '群聊ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `user_name` VARCHAR(100) NOT NULL COMMENT '用户名',
    `content` TEXT COMMENT '消息内容',
    `media_type` VARCHAR(50) DEFAULT 'text' COMMENT '媒体类型',
    `media_url` VARCHAR(500) DEFAULT NULL COMMENT '媒体URL',
    `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_group_id (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息';

CREATE TABLE IF NOT EXISTS `friend_recommendation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `recommend_user_id` BIGINT NOT NULL COMMENT '推荐用户ID',
    `reason` VARCHAR(200) DEFAULT NULL COMMENT '推荐原因',
    `score` DOUBLE DEFAULT 0 COMMENT '匹配分数',
    `viewed` BOOLEAN DEFAULT FALSE COMMENT '是否已查看',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友推荐';

CREATE TABLE IF NOT EXISTS `community_activity` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `community_id` BIGINT NOT NULL COMMENT '社群ID',
    `title` VARCHAR(200) NOT NULL COMMENT '活动标题',
    `description` TEXT DEFAULT NULL COMMENT '活动描述',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `location` VARCHAR(500) DEFAULT NULL COMMENT '活动地点',
    `participant_count` INT DEFAULT 0 COMMENT '参与人数',
    `status` VARCHAR(20) DEFAULT 'upcoming' COMMENT '状态(upcoming/ongoing/completed)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_community_id (`community_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社群活动';

CREATE TABLE IF NOT EXISTS `community_activity_participant` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `participated_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '参与时间',
    INDEX idx_activity_id (`activity_id`),
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动参与者';

CREATE TABLE IF NOT EXISTS `community_rank` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `community_id` BIGINT NOT NULL COMMENT '社群ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `score` INT DEFAULT 0 COMMENT '活跃分数',
    `rank` INT DEFAULT 0 COMMENT '排名',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_community_id (`community_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社群排名';

CREATE TABLE IF NOT EXISTS `topic_tag` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `tag_description` VARCHAR(500) DEFAULT NULL COMMENT '标签描述',
    `post_count` INT DEFAULT 0 COMMENT '关联帖子数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX uk_tag_name (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题标签';

CREATE TABLE IF NOT EXISTS `post_topic_relation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_post_id (`post_id`),
    INDEX idx_tag_id (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子标签关联';

CREATE TABLE IF NOT EXISTS `content_collection` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `content_type` VARCHAR(50) NOT NULL COMMENT '内容类型(post/character/community)',
    `content_id` BIGINT NOT NULL COMMENT '内容ID',
    `collected_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容收藏';

CREATE TABLE IF NOT EXISTS `trending_topic` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `hot_score` INT DEFAULT 0 COMMENT '热度分数',
    `post_count_24h` INT DEFAULT 0 COMMENT '24小时帖子数',
    `rank` INT DEFAULT 0 COMMENT '排名',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_tag_id (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热门话题';

ALTER TABLE `social_post` ADD COLUMN IF NOT EXISTS `topic_tags` TEXT DEFAULT NULL COMMENT '话题标签(JSON)';