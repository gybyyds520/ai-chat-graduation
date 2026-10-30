-- 广告表
CREATE TABLE IF NOT EXISTS advertisement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT '广告标题',
    content TEXT COMMENT '广告内容',
    link_url VARCHAR(512) COMMENT '广告链接',
    image_url VARCHAR(512) COMMENT '广告图片URL',
    position VARCHAR(50) NOT NULL DEFAULT 'HOME_BANNER' COMMENT '广告位置',
    status VARCHAR(20) NOT NULL DEFAULT 'DISABLED' COMMENT '状态: ENABLED/DISABLED',
    priority INT DEFAULT 100 COMMENT '排序优先级',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    click_count INT DEFAULT 0 COMMENT '点击次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT COMMENT '创建人ID',
    INDEX idx_position_status (position, status),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告表';

-- 广告位置枚举说明：
-- HOME_BANNER - 首页横幅
-- LOGIN_PAGE - 登录页面
-- REGISTER_PAGE - 注册页面
-- CHAT_SIDEBAR - 聊天侧边栏
-- FOOTER - 页脚