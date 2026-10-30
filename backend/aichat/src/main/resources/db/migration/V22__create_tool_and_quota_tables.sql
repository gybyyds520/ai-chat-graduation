-- B组可选增强：工具调用日志表 + 用户每日配额表 + 用户会员分级字段

CREATE TABLE IF NOT EXISTS tool_call_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT COMMENT '调用用户ID',
    tool_name VARCHAR(100) COMMENT '工具名称',
    parameters TEXT COMMENT '入参JSON',
    result TEXT COMMENT '结果JSON',
    success TINYINT(1) DEFAULT 1 COMMENT '是否成功',
    duration_ms BIGINT COMMENT '耗时毫秒',
    created_at DATETIME COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_tool_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具调用日志';

CREATE TABLE IF NOT EXISTS user_quota (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT COMMENT '用户ID',
    feature_type VARCHAR(50) COMMENT '功能类型 ai_chat/voice_call/image_gen/doc_upload',
    used_count INT DEFAULT 0 COMMENT '当日已用次数',
    limit_count INT DEFAULT 0 COMMENT '当日上限',
    reset_date DATE COMMENT '配额所属自然日',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_feature_date (user_id, feature_type, reset_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日用量配额';

-- 用户表新增会员分级字段（幂等）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_NAME = 'user' AND COLUMN_NAME = 'membership_level');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE user ADD COLUMN membership_level VARCHAR(20) DEFAULT ''FREE'' COMMENT ''会员等级 FREE/BASIC/PREMIUM''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists2 := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_NAME = 'user' AND COLUMN_NAME = 'membership_expire_at');
SET @sql2 := IF(@col_exists2 = 0,
    'ALTER TABLE user ADD COLUMN membership_expire_at DATETIME NULL COMMENT ''会员到期时间''',
    'SELECT 1');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
