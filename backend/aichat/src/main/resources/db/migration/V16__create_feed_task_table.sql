-- AI 投喂任务表：存储用户真实投喂记录，替代之前 FeedController 中的 Mock 数据。
-- 字段对齐前端 FeedView 的 contentType / priority / processMode / tags / remark / status / progress。
CREATE TABLE IF NOT EXISTS `feed_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id` VARCHAR(64) NOT NULL COMMENT '任务编号(FEED-时间戳)',
    `user_id` BIGINT DEFAULT NULL COMMENT '提交用户ID',
    `content_type` VARCHAR(20) NOT NULL COMMENT '内容类型: text/file/image',
    `content` TEXT DEFAULT NULL COMMENT '文本内容(text类型时)',
    `file_path` VARCHAR(500) DEFAULT NULL COMMENT '上传文件/图片路径(file/image类型时)',
    `file_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    `priority` VARCHAR(20) DEFAULT 'medium' COMMENT '优先级: low/medium/high/urgent',
    `process_mode` VARCHAR(20) DEFAULT 'standard' COMMENT '处理模式: fast/standard/deep',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签(逗号分隔)',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `status` VARCHAR(20) NOT NULL DEFAULT 'completed' COMMENT '状态: pending/processing/completed/failed',
    `progress` INT NOT NULL DEFAULT 100 COMMENT '进度百分比',
    `effectiveness` INT DEFAULT NULL COMMENT '学习效果百分比',
    `duration` BIGINT DEFAULT NULL COMMENT '处理耗时(毫秒)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `started_at` DATETIME DEFAULT NULL COMMENT '开始处理时间',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_feed_task_id` (`task_id`),
    KEY `idx_feed_user_id` (`user_id`),
    KEY `idx_feed_status` (`status`),
    KEY `idx_feed_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI投喂任务记录表';
