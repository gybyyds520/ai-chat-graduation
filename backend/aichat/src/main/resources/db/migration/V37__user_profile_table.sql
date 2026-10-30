-- ============================================================
-- V37: 用户画像表 user_profile
-- 阶段三 P2 新增：基于对话行为沉淀轻量用户画像。
--   user_id           唯一，一个用户一条画像
--   preferred_style   对话风格偏好（concise/detailed/funny/...）
--   common_topics     常见话题，JSON 数组字符串
--   active_time_slots 活跃时段统计，JSON 对象字符串
-- 幂等写法：CREATE TABLE IF NOT EXISTS，可重复执行。
-- ============================================================

CREATE TABLE IF NOT EXISTS `user_profile` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT       NOT NULL COMMENT '所属用户 ID',
    `preferred_style`   VARCHAR(32)  DEFAULT NULL COMMENT '对话风格偏好',
    `common_topics`     TEXT         COMMENT '常见话题 JSON 数组',
    `active_time_slots` TEXT         COMMENT '活跃时段 JSON 对象',
    `created_at`        DATETIME     DEFAULT NULL COMMENT '创建时间',
    `updated_at`        DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_profile_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像（简单版）';
