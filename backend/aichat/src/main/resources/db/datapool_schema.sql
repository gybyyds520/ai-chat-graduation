-- 多AI系统共享数据池数据库设计
-- 创建时间: 2026-06-03

-- 1. AI系统注册表
CREATE TABLE IF NOT EXISTS ai_system (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI系统ID',
    system_name VARCHAR(100) NOT NULL UNIQUE COMMENT '系统名称',
    system_type VARCHAR(50) NOT NULL COMMENT '系统类型(chatbot/assistant/analytics等)',
    api_key VARCHAR(255) NOT NULL UNIQUE COMMENT 'API密钥(加密存储)',
    secret_key VARCHAR(255) NOT NULL COMMENT '密钥(加密存储)',
    description TEXT COMMENT '系统描述',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    rate_limit INT DEFAULT 100 COMMENT '每分钟请求限制',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_access_at DATETIME COMMENT '最后访问时间',
    INDEX idx_system_name (system_name),
    INDEX idx_api_key (api_key),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI系统注册表';

-- 2. 数据池主表
CREATE TABLE IF NOT EXISTS data_pool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '数据ID',
    title VARCHAR(255) NOT NULL COMMENT '数据标题',
    content LONGTEXT NOT NULL COMMENT '数据内容',
    data_type VARCHAR(50) NOT NULL COMMENT '数据类型(text/json/xml/csv/image/audio/video/document等)',
    format VARCHAR(20) NOT NULL COMMENT '数据格式',
    file_path VARCHAR(500) COMMENT '文件存储路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    checksum VARCHAR(64) COMMENT '数据校验和(MD5/SHA256)',
    category VARCHAR(100) COMMENT '数据分类',
    description TEXT COMMENT '数据描述',
    metadata JSON COMMENT '元数据(键值对)',
    version INT DEFAULT 1 COMMENT '版本号',
    is_public TINYINT DEFAULT 0 COMMENT '是否公开: 1-公开, 0-私有',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用, 2-归档',
    created_by VARCHAR(100) COMMENT '创建者',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    expires_at DATETIME COMMENT '过期时间',
    INDEX idx_data_type (data_type),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_is_public (is_public),
    INDEX idx_created_at (created_at),
    FULLTEXT INDEX ft_content (content, title, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据池主表';

-- 3. 数据标签表
CREATE TABLE IF NOT EXISTS data_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    tag_name VARCHAR(100) NOT NULL UNIQUE COMMENT '标签名称',
    tag_type VARCHAR(50) DEFAULT 'general' COMMENT '标签类型',
    description VARCHAR(500) COMMENT '标签描述',
    color VARCHAR(20) COMMENT '标签颜色',
    parent_id BIGINT COMMENT '父标签ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_tag_name (tag_name),
    INDEX idx_tag_type (tag_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据标签表';

-- 4. 数据标签关联表
CREATE TABLE IF NOT EXISTS data_tag_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_id BIGINT NOT NULL COMMENT '数据ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_data_tag (data_id, tag_id),
    INDEX idx_data_id (data_id),
    INDEX idx_tag_id (tag_id),
    FOREIGN KEY (data_id) REFERENCES data_pool(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES data_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据标签关联表';

-- 5. 访问权限表
CREATE TABLE IF NOT EXISTS data_access_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_id BIGINT COMMENT '数据ID(NULL表示全局权限)',
    ai_system_id BIGINT COMMENT 'AI系统ID(NULL表示公开数据)',
    permission_type ENUM('read', 'write', 'delete', 'admin') DEFAULT 'read' COMMENT '权限类型',
    data_category VARCHAR(100) COMMENT '数据分类权限(与data_id二选一)',
    expires_at DATETIME COMMENT '权限过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by VARCHAR(100) COMMENT '授权人',
    INDEX idx_data_id (data_id),
    INDEX idx_ai_system_id (ai_system_id),
    INDEX idx_permission_type (permission_type),
    FOREIGN KEY (data_id) REFERENCES data_pool(id) ON DELETE CASCADE,
    FOREIGN KEY (ai_system_id) REFERENCES ai_system(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问权限表';

-- 6. 数据版本控制表
CREATE TABLE IF NOT EXISTS data_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_id BIGINT NOT NULL COMMENT '数据ID',
    version INT NOT NULL COMMENT '版本号',
    content LONGTEXT COMMENT '历史内容',
    change_summary TEXT COMMENT '变更摘要',
    changed_by VARCHAR(100) COMMENT '变更人',
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    UNIQUE KEY uk_data_version (data_id, version),
    INDEX idx_data_id (data_id),
    FOREIGN KEY (data_id) REFERENCES data_pool(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据版本控制表';

-- 7. 访问日志表
CREATE TABLE IF NOT EXISTS access_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ai_system_id BIGINT COMMENT 'AI系统ID',
    data_id BIGINT COMMENT '访问的数据ID',
    access_type VARCHAR(50) NOT NULL COMMENT '访问类型（审计动作如 LOGIN/ADMIN_LOGIN/REGISTER，及 read/write/delete/query/upload/download）',
    request_method VARCHAR(10) COMMENT 'HTTP方法',
    request_path VARCHAR(500) COMMENT '请求路径',
    request_params TEXT COMMENT '请求参数',
    response_status INT COMMENT '响应状态码',
    response_time INT COMMENT '响应时间(毫秒)',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    error_message TEXT COMMENT '错误信息',
    accessed_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    INDEX idx_ai_system_id (ai_system_id),
    INDEX idx_data_id (data_id),
    INDEX idx_access_type (access_type),
    INDEX idx_accessed_at (accessed_at),
    INDEX idx_response_status (response_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问日志表';

-- 8. 数据分类表
CREATE TABLE IF NOT EXISTS data_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL UNIQUE COMMENT '分类名称',
    category_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类代码',
    description TEXT COMMENT '分类描述',
    parent_id BIGINT COMMENT '父分类ID',
    icon VARCHAR(100) COMMENT '分类图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_category_name (category_name),
    INDEX idx_category_code (category_code),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据分类表';

-- 9. 数据导入任务表
CREATE TABLE IF NOT EXISTS data_import_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name VARCHAR(255) NOT NULL COMMENT '任务名称',
    import_type ENUM('api', 'file', 'manual', 'batch') NOT NULL COMMENT '导入类型',
    file_path VARCHAR(500) COMMENT '文件路径',
    file_format VARCHAR(20) COMMENT '文件格式',
    total_count INT DEFAULT 0 COMMENT '总数量',
    success_count INT DEFAULT 0 COMMENT '成功数量',
    fail_count INT DEFAULT 0 COMMENT '失败数量',
    status ENUM('pending', 'processing', 'completed', 'failed') DEFAULT 'pending' COMMENT '任务状态',
    error_log TEXT COMMENT '错误日志',
    started_at DATETIME COMMENT '开始时间',
    completed_at DATETIME COMMENT '完成时间',
    created_by VARCHAR(100) COMMENT '创建者',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_status (status),
    INDEX idx_import_type (import_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据导入任务表';

-- 10. 数据缓存表(用于加速查询)
CREATE TABLE IF NOT EXISTS data_cache (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cache_key VARCHAR(255) NOT NULL UNIQUE COMMENT '缓存键',
    cache_value LONGTEXT COMMENT '缓存值',
    data_type VARCHAR(50) COMMENT '数据类型',
    expires_at DATETIME COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_cache_key (cache_key),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据缓存表';

-- 插入默认数据分类
INSERT INTO data_category (category_name, category_code, description, icon, sort_order) VALUES
('文本数据', 'text', '纯文本、文章、文档等', 'document', 1),
('结构化数据', 'structured', 'JSON、XML、CSV等结构化数据', 'table', 2),
('图像数据', 'image', '图片、图表等视觉数据', 'image', 3),
('音频数据', 'audio', '语音、音乐等音频数据', 'audio', 4),
('视频数据', 'video', '视频、动画等多媒体数据', 'video', 5),
('知识库', 'knowledge', '知识图谱、FAQ、百科等', 'book', 6),
('模型数据', 'model', 'AI模型、参数、权重等', 'cube', 7),
('配置数据', 'config', '系统配置、参数设置等', 'setting', 8),
('日志数据', 'log', '系统日志、访问记录等', 'log', 9),
('其他数据', 'other', '其他类型数据', 'folder', 10);

-- 插入默认标签
INSERT INTO data_tag (tag_name, tag_type, description, color) VALUES
('重要', 'priority', '重要数据', '#f56c6c'),
('公开', 'access', '公开数据', '#67c23a'),
('机密', 'security', '机密数据', '#e6a23c'),
('测试', 'status', '测试数据', '#909399'),
('已验证', 'quality', '已验证数据', '#409eff');
