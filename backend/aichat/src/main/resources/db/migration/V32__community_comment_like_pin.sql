-- ============================================================
-- V32: 兴趣社群功能完善
--   1. community_post_comment 表（帖子评论）
--   2. community_post_like 表（点赞去重，post_id + user_id 唯一索引）
--   3. community_post 增加 is_pinned 列（管理员置顶）
-- 幂等：表已存在则跳过建表；列已存在则跳过加列
-- ============================================================

-- 1) 帖子评论表
CREATE TABLE IF NOT EXISTS `community_post_comment` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT COMMENT '评论者ID',
    user_name VARCHAR(100) COMMENT '评论者昵称',
    content VARCHAR(1000) COMMENT '评论内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社群帖子评论表';

-- 2) 帖子点赞表（去重）
CREATE TABLE IF NOT EXISTS `community_post_like` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_post_user (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社群帖子点赞记录表';

-- 3) community_post 增加 is_pinned 列（幂等）
SET @has_community_post_is_pinned := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_post' AND COLUMN_NAME = 'is_pinned');
SET @ddl_community_post_is_pinned := IF(@has_community_post_is_pinned = 0, 'ALTER TABLE `community_post` ADD COLUMN `is_pinned` TINYINT(1) DEFAULT 0 COMMENT ''是否置顶'' AFTER `is_deleted`', 'SELECT 1');
PREPARE s_community_post_is_pinned FROM @ddl_community_post_is_pinned;
EXECUTE s_community_post_is_pinned;
DEALLOCATE PREPARE s_community_post_is_pinned;
