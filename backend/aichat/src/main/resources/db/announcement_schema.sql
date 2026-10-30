-- 公告表
CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告内容',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED',
    priority INT DEFAULT 1 COMMENT '优先级: 0-低, 1-中, 2-高, 3-紧急',
    publish_time DATETIME COMMENT '发布时间',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    display_scope VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '显示范围: ALL/LOGGED_IN/ADMIN',
    author VARCHAR(100) COMMENT '作者',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT COMMENT '创建人ID',
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_publish_time (publish_time),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time),
    INDEX idx_display_scope (display_scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

-- 状态枚举说明：
-- DRAFT - 草稿
-- PUBLISHED - 已发布

-- 显示范围枚举说明：
-- ALL - 所有用户
-- LOGGED_IN - 仅登录用户
-- ADMIN - 仅管理员

-- 优先级枚举说明：
-- 0 - 低
-- 1 - 中
-- 2 - 高
-- 3 - 紧急