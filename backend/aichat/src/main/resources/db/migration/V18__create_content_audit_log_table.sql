-- 内容审核日志表：记录全平台主动审核的每次结果，供管理后台查询与统计。
-- 写路径（帖子/评论/私聊/群聊/AI 角色/举报）在内容入库前审核，结果同步落库。
CREATE TABLE IF NOT EXISTS `content_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT DEFAULT NULL COMMENT '提交内容的用户ID',
    `content_type` VARCHAR(32) NOT NULL COMMENT '内容类型: post/comment/private_chat/group_chat/ai_character/report/ai_generated',
    `target_id` BIGINT DEFAULT NULL COMMENT '被审核内容对象ID',
    `content_snapshot` VARCHAR(500) DEFAULT NULL COMMENT '审核内容快照(超长截断)',
    `risk_level` VARCHAR(16) NOT NULL DEFAULT 'LOW' COMMENT '风险等级: LOW/MEDIUM/HIGH',
    `matched_words` VARCHAR(500) DEFAULT NULL COMMENT '命中敏感词,逗号分隔',
    `passed` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否通过',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_audit_user_id` (`user_id`),
    KEY `idx_audit_content_type` (`content_type`),
    KEY `idx_audit_risk_level` (`risk_level`),
    KEY `idx_audit_passed` (`passed`),
    KEY `idx_audit_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核日志表';
