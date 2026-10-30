ALTER TABLE `ai_character` ADD COLUMN IF NOT EXISTS `personality` TEXT DEFAULT NULL COMMENT '性格描述';
ALTER TABLE `ai_character` ADD COLUMN IF NOT EXISTS `voice_style` VARCHAR(100) DEFAULT 'neutral' COMMENT '语音风格(neutral/female/male/child/robot)';
ALTER TABLE `ai_character` ADD COLUMN IF NOT EXISTS `emotion_base` VARCHAR(50) DEFAULT 'happy' COMMENT '基础情感(happy/sad/angry/surprised/fearful/disgusted/neutral)';
ALTER TABLE `ai_character` ADD COLUMN IF NOT EXISTS `expressions` TEXT DEFAULT NULL COMMENT '可用表情列表(JSON)';
ALTER TABLE `ai_character` ADD COLUMN IF NOT EXISTS `actions` TEXT DEFAULT NULL COMMENT '可用动作列表(JSON)';
ALTER TABLE `ai_character` ADD COLUMN IF NOT EXISTS `backstory` TEXT DEFAULT NULL COMMENT '角色背景故事';
ALTER TABLE `ai_character` ADD COLUMN IF NOT EXISTS `relationship` VARCHAR(100) DEFAULT 'friend' COMMENT '与用户关系(friend/lover/teacher/assistant)';

CREATE TABLE IF NOT EXISTS `character_emotion` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `character_id` BIGINT NOT NULL COMMENT '角色ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `emotion_type` VARCHAR(50) NOT NULL COMMENT '情感类型(happy/sad/angry/surprised/fearful/disgusted/neutral/love/hate/joy/sorrow)';
    `intensity` DOUBLE DEFAULT 0.5 COMMENT '情感强度(0-1)';
    `trigger_message` TEXT COMMENT '触发消息';
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_character_id (`character_id`),
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色情感记录';

CREATE TABLE IF NOT EXISTS `character_expression` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `character_id` BIGINT NOT NULL COMMENT '角色ID',
    `expression_name` VARCHAR(100) NOT NULL COMMENT '表情名称',
    `expression_code` VARCHAR(50) NOT NULL COMMENT '表情代码(如: smile, cry, angry)';
    `description` VARCHAR(500) DEFAULT NULL COMMENT '表情描述',
    `emoji` VARCHAR(10) DEFAULT NULL COMMENT '对应emoji',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_character_id (`character_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表情库';

CREATE TABLE IF NOT EXISTS `character_action` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `character_id` BIGINT NOT NULL COMMENT '角色ID',
    `action_name` VARCHAR(100) NOT NULL COMMENT '动作名称',
    `action_code` VARCHAR(50) NOT NULL COMMENT '动作代码(如: wave, jump, dance)';
    `description` VARCHAR(500) DEFAULT NULL COMMENT '动作描述',
    `emoji_sequence` VARCHAR(100) DEFAULT NULL COMMENT '动作emoji序列',
    `duration_ms` INT DEFAULT 1000 COMMENT '动作持续时间(毫秒)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_character_id (`character_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色动作库';

CREATE TABLE IF NOT EXISTS `conversation_context` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `session_id` VARCHAR(100) NOT NULL COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `character_id` BIGINT NOT NULL COMMENT '角色ID',
    `context_key` VARCHAR(100) NOT NULL COMMENT '上下文键',
    `context_value` TEXT COMMENT '上下文值',
    `expire_time` DATETIME COMMENT '过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (`session_id`),
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话上下文记忆';

CREATE TABLE IF NOT EXISTS `ai_elf` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '小精灵名称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '小精灵头像',
    `theme` VARCHAR(50) DEFAULT 'default' COMMENT '主题风格(default/cute/dark/fantasy)';
    `personality` VARCHAR(200) DEFAULT 'friendly' COMMENT '性格(friendly/shy/energetic/calm)';
    `level` INT DEFAULT 1 COMMENT '等级',
    `experience` INT DEFAULT 0 COMMENT '经验值',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI小精灵';

CREATE TABLE IF NOT EXISTS `ai_elf_alarm` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `elf_id` BIGINT NOT NULL COMMENT '小精灵ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '闹钟标题',
    `time` TIME NOT NULL COMMENT '闹钟时间',
    `repeat_days` VARCHAR(50) DEFAULT 'daily' COMMENT '重复周期(daily/workday/weekend/mon,tue,wed)';
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `ringtone` VARCHAR(100) DEFAULT 'default' COMMENT '铃声',
    `message` TEXT DEFAULT NULL COMMENT '提醒消息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_elf_id (`elf_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小精灵闹钟';

CREATE TABLE IF NOT EXISTS `ai_elf_schedule` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `elf_id` BIGINT NOT NULL COMMENT '小精灵ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '日程标题',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `location` VARCHAR(500) DEFAULT NULL COMMENT '地点',
    `description` TEXT DEFAULT NULL COMMENT '日程描述',
    `reminder_minutes` INT DEFAULT 15 COMMENT '提前提醒分钟数',
    `completed` BOOLEAN DEFAULT FALSE COMMENT '是否完成',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_elf_id (`elf_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小精灵日程';

CREATE TABLE IF NOT EXISTS `ai_elf_sleep` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `elf_id` BIGINT NOT NULL COMMENT '小精灵ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `sleep_time` TIME NOT NULL COMMENT '睡觉时间',
    `wake_time` TIME NOT NULL COMMENT '起床时间',
    `quality` INT DEFAULT 0 COMMENT '睡眠质量(0-10)',
    `duration_hours` DOUBLE DEFAULT 0 COMMENT '睡眠时长(小时)',
    `notes` TEXT DEFAULT NULL COMMENT '睡眠备注',
    `record_date` DATE NOT NULL COMMENT '记录日期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (`elf_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小精灵睡眠管理';

CREATE TABLE IF NOT EXISTS `interest_community` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `name` VARCHAR(200) NOT NULL COMMENT '社群名称',
    `description` TEXT DEFAULT NULL COMMENT '社群描述',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '社群头像',
    `category` VARCHAR(100) DEFAULT 'other' COMMENT '分类(tech/gaming/music/art/study/sports/other)';
    `owner_id` BIGINT NOT NULL COMMENT '创建者ID',
    `member_count` INT DEFAULT 1 COMMENT '成员数量',
    `post_count` INT DEFAULT 0 COMMENT '帖子数量',
    `visibility` VARCHAR(20) DEFAULT 'public' COMMENT '可见性(public/private)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_owner_id (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='兴趣社群';

CREATE TABLE IF NOT EXISTS `community_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `community_id` BIGINT NOT NULL COMMENT '社群ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(50) DEFAULT 'member' COMMENT '角色(owner/admin/member)';
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    INDEX idx_community_id (`community_id`),
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社群成员';

CREATE TABLE IF NOT EXISTS `community_post` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `community_id` BIGINT NOT NULL COMMENT '社群ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `user_name` VARCHAR(100) NOT NULL COMMENT '用户名',
    `content` TEXT COMMENT '内容',
    `media_type` VARCHAR(50) DEFAULT 'text' COMMENT '媒体类型(text/image/video)';
    `media_urls` TEXT DEFAULT NULL COMMENT '媒体URL(JSON)',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `is_deleted` BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_community_id (`community_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社群帖子';

CREATE TABLE IF NOT EXISTS `character_showcase` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `character_id` BIGINT NOT NULL COMMENT '角色ID',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '展示标题',
    `description` TEXT DEFAULT NULL COMMENT '展示描述',
    `visibility` VARCHAR(20) DEFAULT 'public' COMMENT '可见性(public/friends/private)';
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色展示';

CREATE TABLE IF NOT EXISTS `character_interaction` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `character_id` BIGINT NOT NULL COMMENT '角色ID',
    `target_user_id` BIGINT NOT NULL COMMENT '目标用户ID',
    `interaction_type` VARCHAR(50) NOT NULL COMMENT '互动类型(visit/gift/chat/duel)';
    `message` TEXT DEFAULT NULL COMMENT '互动消息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色社交互动';

ALTER TABLE `character_emotion` MODIFY COLUMN `emotion_type` VARCHAR(50) NOT NULL COMMENT '情感类型';
ALTER TABLE `character_expression` MODIFY COLUMN `expression_code` VARCHAR(50) NOT NULL COMMENT '表情代码';
ALTER TABLE `character_action` MODIFY COLUMN `action_code` VARCHAR(50) NOT NULL COMMENT '动作代码';
ALTER TABLE `ai_elf` MODIFY COLUMN `theme` VARCHAR(50) DEFAULT 'default' COMMENT '主题风格';
ALTER TABLE `ai_elf_alarm` MODIFY COLUMN `repeat_days` VARCHAR(50) DEFAULT 'daily' COMMENT '重复周期';
ALTER TABLE `interest_community` MODIFY COLUMN `category` VARCHAR(100) DEFAULT 'other' COMMENT '分类';
ALTER TABLE `community_member` MODIFY COLUMN `role` VARCHAR(50) DEFAULT 'member' COMMENT '角色';
ALTER TABLE `community_post` MODIFY COLUMN `media_type` VARCHAR(50) DEFAULT 'text' COMMENT '媒体类型';
ALTER TABLE `character_showcase` MODIFY COLUMN `visibility` VARCHAR(20) DEFAULT 'public' COMMENT '可见性';
ALTER TABLE `character_interaction` MODIFY COLUMN `interaction_type` VARCHAR(50) NOT NULL COMMENT '互动类型';

-- M1 ②：用户级定时提醒（星野式留存）：到点由定时任务经 WebSocket 推送给在线用户
CREATE TABLE IF NOT EXISTS `elf_reminder` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) DEFAULT NULL COMMENT '提醒标题',
    content VARCHAR(1000) DEFAULT NULL COMMENT '提醒正文',
    remind_time DATETIME NOT NULL COMMENT '触发时间',
    triggered TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已触发',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_remind_time (remind_time),
    INDEX idx_triggered (triggered)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='精灵定时提醒表';