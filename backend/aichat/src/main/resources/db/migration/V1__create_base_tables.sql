CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    avatar_url VARCHAR(500) COMMENT '头像完整URL',
    email VARCHAR(255) COMMENT '邮箱',
    bio TEXT COMMENT '个人简介',
    interests VARCHAR(500) COMMENT '兴趣爱好',
    location VARCHAR(100) COMMENT '所在地',
    website VARCHAR(200) COMMENT '个人网站',
    gender VARCHAR(10) COMMENT '性别',
    birthday VARCHAR(20) COMMENT '生日',
    version_type VARCHAR(20) DEFAULT 'FREE' COMMENT '版本类型(FREE/PRO)',
    version_expire_time DATETIME COMMENT '版本过期时间',
    account_status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '账户状态(ACTIVE/LOCKED)',
    phone VARCHAR(20) COMMENT '手机号',
    phone_verified BOOLEAN DEFAULT FALSE COMMENT '手机是否验证',
    email_verified BOOLEAN DEFAULT FALSE COMMENT '邮箱是否验证',
    mfa_enabled BOOLEAN DEFAULT FALSE COMMENT '是否启用MFA',
    mfa_secret VARCHAR(100) COMMENT 'MFA密钥',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    last_login_time DATETIME COMMENT '最后登录时间',
    failed_login_count INT DEFAULT 0 COMMENT '登录失败次数',
    lock_time DATETIME COMMENT '锁定时间',
    security_question VARCHAR(200) COMMENT '安全问题',
    security_answer VARCHAR(200) COMMENT '安全答案',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_account_status (account_status),
    INDEX idx_version_type (version_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS ai_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_key VARCHAR(100) NOT NULL COMMENT '模型标识key',
    provider VARCHAR(50) NOT NULL COMMENT '模型提供商',
    description TEXT COMMENT '模型描述',
    cost_per_token DOUBLE DEFAULT 0 COMMENT '每token成本',
    max_tokens INT DEFAULT 4096 COMMENT '最大token数',
    available BOOLEAN DEFAULT TRUE COMMENT '是否可用',
    category VARCHAR(50) DEFAULT 'chat' COMMENT '模型分类',
    priority INT DEFAULT 0 COMMENT '优先级',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_model_key (model_key),
    INDEX idx_provider (provider),
    INDEX idx_available (available)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型表';

CREATE TABLE IF NOT EXISTS ai_character (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    avatar VARCHAR(255) COMMENT '角色头像',
    prompt TEXT NOT NULL COMMENT '角色提示词',
    model VARCHAR(100) DEFAULT 'phi3:mini' COMMENT '模型名称',
    personality TEXT COMMENT '性格描述',
    voice_style VARCHAR(100) COMMENT '语音风格',
    emotion_base VARCHAR(100) COMMENT '情感基调',
    expressions TEXT COMMENT '表情描述',
    actions TEXT COMMENT '动作描述',
    backstory TEXT COMMENT '背景故事',
    relationship VARCHAR(100) COMMENT '与用户关系',
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否系统角色',
    is_shared BOOLEAN DEFAULT FALSE COMMENT '是否分享',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_is_system (is_system),
    INDEX idx_is_shared (is_shared)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI角色表';

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    character_id BIGINT COMMENT '角色ID',
    model VARCHAR(100) COMMENT '使用模型',
    content TEXT COMMENT '消息内容',
    role VARCHAR(20) DEFAULT 'user' COMMENT '角色(user/assistant/system)',
    chat_type VARCHAR(20) DEFAULT 'single' COMMENT '聊天类型',
    conversation_id VARCHAR(100) COMMENT '会话ID',
    message_order INT DEFAULT 0 COMMENT '消息顺序',
    tokens_used INT DEFAULT 0 COMMENT '使用token数',
    duration_ms INT DEFAULT 0 COMMENT '响应时间(毫秒)',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_character_id (character_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

CREATE TABLE IF NOT EXISTS friendship (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/ACCEPTED/REJECTED)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_friend_id (friend_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

CREATE TABLE IF NOT EXISTS friend_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    sender_name VARCHAR(100) COMMENT '发送者昵称（冗余提升查询性能）',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    receiver_name VARCHAR(100) COMMENT '接收者昵称（冗余提升查询性能）',
    content TEXT COMMENT '消息内容',
    message_type VARCHAR(50) DEFAULT 'text' COMMENT '消息类型（与 type 互为兼容字段）',
    type VARCHAR(50) DEFAULT 'text' COMMENT '消息类型（兼容旧代码）',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    read_at DATETIME COMMENT '已读时间',
    file_name VARCHAR(255) COMMENT '文件名（文件消息）',
    file_path VARCHAR(500) COMMENT '文件路径',
    file_size BIGINT COMMENT '文件大小（字节）',
    extra_data TEXT COMMENT '扩展数据（JSON）',
    status INT DEFAULT 0 COMMENT '消息状态（0正常/1撤回/2审核中）',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友聊天消息表';

CREATE TABLE IF NOT EXISTS group_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '群名称',
    avatar VARCHAR(255) COMMENT '群头像',
    description TEXT COMMENT '群描述',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    max_members INT DEFAULT 200 COMMENT '最大成员数',
    is_public BOOLEAN DEFAULT TRUE COMMENT '是否公开',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_creator_id (creator_id),
    INDEX idx_is_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊表';

CREATE TABLE IF NOT EXISTS group_chat_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    group_id BIGINT NOT NULL COMMENT '群ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) DEFAULT 'member' COMMENT '角色(owner/admin/member)',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否已退出',
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

CREATE TABLE IF NOT EXISTS group_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    group_id BIGINT NOT NULL COMMENT '群ID',
    user_id BIGINT NOT NULL COMMENT '发送者ID',
    content TEXT COMMENT '消息内容',
    type VARCHAR(50) DEFAULT 'text' COMMENT '消息类型',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息表';

CREATE TABLE IF NOT EXISTS social_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    user_name VARCHAR(100) COMMENT '用户名',
    content TEXT COMMENT '内容',
    media_type VARCHAR(50) COMMENT '媒体类型',
    media_urls TEXT COMMENT '媒体URLs(JSON)',
    video_url VARCHAR(500) COMMENT '视频URL',
    video_cover VARCHAR(500) COMMENT '视频封面',
    location VARCHAR(200) COMMENT '位置',
    visibility VARCHAR(20) DEFAULT 'public' COMMENT '可见性',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    share_count INT DEFAULT 0 COMMENT '分享数',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_visibility (visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社交帖子表';

CREATE TABLE IF NOT EXISTS social_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT COMMENT '用户ID',
    user_name VARCHAR(100) COMMENT '用户名',
    content TEXT COMMENT '评论内容',
    parent_id BIGINT COMMENT '父评论ID',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_post_id (post_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

CREATE TABLE IF NOT EXISTS social_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_post_user (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

CREATE TABLE IF NOT EXISTS social_post_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    INDEX idx_post_id (post_id),
    INDEX idx_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子标签表';

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(50) COMMENT '通知类型',
    target_type VARCHAR(50) COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    actor_id BIGINT COMMENT '操作者ID',
    actor_name VARCHAR(100) COMMENT '操作者名称',
    message TEXT COMMENT '通知消息',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT COMMENT '公告内容',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态(DRAFT/PUBLISHED/ARCHIVED)',
    priority INT DEFAULT 1 COMMENT '优先级',
    publish_time DATETIME COMMENT '发布时间',
    start_time DATETIME COMMENT '开始显示时间',
    end_time DATETIME COMMENT '结束显示时间',
    display_scope VARCHAR(20) DEFAULT 'ALL' COMMENT '显示范围',
    author VARCHAR(100) COMMENT '作者',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建者ID',
    INDEX idx_status (status),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

CREATE TABLE IF NOT EXISTS advertisement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(200) COMMENT '广告标题',
    content TEXT COMMENT '广告内容',
    link_url VARCHAR(500) COMMENT '链接URL',
    image_url VARCHAR(500) COMMENT '图片URL',
    position VARCHAR(50) COMMENT '位置',
    status VARCHAR(20) DEFAULT 'DISABLED' COMMENT '状态',
    priority INT DEFAULT 0 COMMENT '优先级',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    click_count INT DEFAULT 0 COMMENT '点击次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建者ID',
    INDEX idx_position (position),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告表';

CREATE TABLE IF NOT EXISTS ai_elf (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    name VARCHAR(100) COMMENT '精灵名称',
    theme VARCHAR(50) COMMENT '主题',
    level INT DEFAULT 1 COMMENT '等级',
    experience INT DEFAULT 0 COMMENT '经验值',
    energy INT DEFAULT 100 COMMENT '能量值',
    health INT DEFAULT 100 COMMENT '健康值',
    mood INT DEFAULT 50 COMMENT '心情值',
    avatar VARCHAR(255) COMMENT '精灵头像',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI精灵表';

CREATE TABLE IF NOT EXISTS ai_elf_alarm (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    elf_id BIGINT NOT NULL COMMENT '精灵ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(100) COMMENT '标题',
    alarm_time VARCHAR(20) COMMENT '提醒时间',
    repeat_days VARCHAR(50) COMMENT '重复日期',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (elf_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精灵闹钟表';

CREATE TABLE IF NOT EXISTS ai_elf_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    elf_id BIGINT NOT NULL COMMENT '精灵ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(100) COMMENT '标题',
    start_time VARCHAR(20) COMMENT '开始时间',
    end_time VARCHAR(20) COMMENT '结束时间',
    is_completed BOOLEAN DEFAULT FALSE COMMENT '是否完成',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (elf_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精灵日程表';

CREATE TABLE IF NOT EXISTS ai_elf_sleep (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    elf_id BIGINT NOT NULL COMMENT '精灵ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    sleep_time VARCHAR(20) COMMENT '入睡时间',
    wake_time VARCHAR(20) COMMENT '醒来时间',
    quality INT DEFAULT 0 COMMENT '睡眠质量(1-10)',
    duration_hours DOUBLE DEFAULT 0 COMMENT '睡眠时长(小时)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (elf_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精灵睡眠记录表';

CREATE TABLE IF NOT EXISTS interest_community (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '社群名称',
    description TEXT COMMENT '社群描述',
    avatar VARCHAR(255) COMMENT '社群头像',
    category VARCHAR(50) COMMENT '分类',
    owner_id BIGINT COMMENT '创建者ID',
    member_count INT DEFAULT 0 COMMENT '成员数',
    post_count INT DEFAULT 0 COMMENT '帖子数',
    visibility VARCHAR(20) DEFAULT 'public' COMMENT '可见性',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_owner_id (owner_id),
    INDEX idx_category (category),
    INDEX idx_visibility (visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='兴趣社群表';

CREATE TABLE IF NOT EXISTS community_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    community_id BIGINT NOT NULL COMMENT '社群ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) DEFAULT 'member' COMMENT '角色',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    INDEX idx_community_id (community_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社群成员表';

CREATE TABLE IF NOT EXISTS community_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    community_id BIGINT NOT NULL COMMENT '社群ID',
    user_id BIGINT COMMENT '用户ID',
    user_name VARCHAR(100) COMMENT '用户名',
    content TEXT COMMENT '内容',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_community_id (community_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社群帖子表';

CREATE TABLE IF NOT EXISTS character_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    character_id BIGINT NOT NULL COMMENT '角色ID',
    from_user_id BIGINT COMMENT '分享者ID',
    to_user_id BIGINT COMMENT '被分享者ID',
    permissions VARCHAR(50) DEFAULT 'read' COMMENT '权限',
    shared_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分享时间',
    INDEX idx_character_id (character_id),
    INDEX idx_to_user_id (to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色分享表';

CREATE TABLE IF NOT EXISTS user_data_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    data_type VARCHAR(50) NOT NULL COMMENT '数据类型',
    content TEXT COMMENT '数据内容',
    metadata TEXT COMMENT '元数据(JSON)',
    tags VARCHAR(500) COMMENT '标签',
    integrity_score DOUBLE DEFAULT 0.5 COMMENT '完整性评分',
    status VARCHAR(20) DEFAULT 'PENDING_VALIDATION' COMMENT '状态',
    capture_time DATETIME COMMENT '捕获时间',
    group_id VARCHAR(100) COMMENT '分组ID',
    backup_status VARCHAR(20) DEFAULT 'PENDING_BACKUP' COMMENT '备份状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_data_type (data_type),
    INDEX idx_capture_time (capture_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户数据记录表';

CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_no VARCHAR(100) NOT NULL UNIQUE COMMENT '订单号',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    currency VARCHAR(20) DEFAULT 'CNY' COMMENT '币种',
    payment_method VARCHAR(50) COMMENT '支付方式',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    transaction_id VARCHAR(100) COMMENT '交易ID',
    product_name VARCHAR(200) COMMENT '商品名称',
    product_type VARCHAR(50) COMMENT '商品类型',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

CREATE TABLE IF NOT EXISTS pay_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    account_type VARCHAR(50) COMMENT '账户类型',
    balance DECIMAL(10,2) DEFAULT 0 COMMENT '余额',
    freeze_amount DECIMAL(10,2) DEFAULT 0 COMMENT '冻结金额',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_account_type (account_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付账户表';

CREATE TABLE IF NOT EXISTS login_device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_id VARCHAR(100) NOT NULL COMMENT '设备ID',
    device_name VARCHAR(100) COMMENT '设备名称',
    device_type VARCHAR(50) COMMENT '设备类型',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    location VARCHAR(100) COMMENT '地理位置',
    is_trusted BOOLEAN DEFAULT FALSE COMMENT '是否信任',
    last_login_time DATETIME COMMENT '最后登录时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_device_id (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录设备表';

CREATE TABLE IF NOT EXISTS sms_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    code VARCHAR(10) NOT NULL COMMENT '验证码',
    type VARCHAR(20) COMMENT '类型(register/login/reset)',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    is_used BOOLEAN DEFAULT FALSE COMMENT '是否使用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_phone (phone),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信验证码表';

CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    follow_id BIGINT NOT NULL COMMENT '关注ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_follow_id (follow_id),
    UNIQUE KEY uk_user_follow (user_id, follow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

CREATE TABLE IF NOT EXISTS user_block (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    block_id BIGINT NOT NULL COMMENT '被拉黑ID',
    reason VARCHAR(200) COMMENT '原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_block_id (block_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户拉黑表';

CREATE TABLE IF NOT EXISTS user_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    reporter_id BIGINT NOT NULL COMMENT '举报者ID',
    reported_id BIGINT NOT NULL COMMENT '被举报者ID',
    type VARCHAR(50) COMMENT '举报类型',
    reason VARCHAR(500) COMMENT '举报原因',
    evidence TEXT COMMENT '证据',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_reporter_id (reporter_id),
    INDEX idx_reported_id (reported_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户举报表';

CREATE TABLE IF NOT EXISTS friend_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '分组名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友分组表';

CREATE TABLE IF NOT EXISTS friend_group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    group_id BIGINT NOT NULL COMMENT '分组ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    INDEX idx_group_id (group_id),
    INDEX idx_friend_id (friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友分组成员表';

CREATE TABLE IF NOT EXISTS privacy_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    setting_key VARCHAR(100) NOT NULL COMMENT '设置项',
    setting_value VARCHAR(500) COMMENT '设置值',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='隐私设置表';

CREATE TABLE IF NOT EXISTS user_online_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    is_online BOOLEAN DEFAULT FALSE COMMENT '是否在线',
    last_active_time DATETIME COMMENT '最后活跃时间',
    session_id VARCHAR(100) COMMENT '会话ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_is_online (is_online)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户在线状态表';

CREATE TABLE IF NOT EXISTS data_pool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '数据池名称',
    description TEXT COMMENT '描述',
    data_type VARCHAR(50) COMMENT '数据类型',
    size INT DEFAULT 0 COMMENT '数据量',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建者ID',
    INDEX idx_data_type (data_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据池表';

CREATE TABLE IF NOT EXISTS data_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    pool_id BIGINT NOT NULL COMMENT '数据池ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    tag_value VARCHAR(200) COMMENT '标签值',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_pool_id (pool_id),
    INDEX idx_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据标签表';

CREATE TABLE IF NOT EXISTS data_access_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    pool_id BIGINT NOT NULL COMMENT '数据池ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    permission_type VARCHAR(20) DEFAULT 'read' COMMENT '权限类型',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    granted_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    INDEX idx_pool_id (pool_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据访问权限表';

CREATE TABLE IF NOT EXISTS access_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    endpoint VARCHAR(200) COMMENT '访问端点',
    method VARCHAR(10) COMMENT '请求方法',
    status_code INT COMMENT '状态码',
    response_time_ms INT COMMENT '响应时间(毫秒)',
    user_agent VARCHAR(500) COMMENT '用户代理',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_endpoint (endpoint),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问日志表';

-- 登录风险评估表（V4 插入测试数据前需先创建）
CREATE TABLE IF NOT EXISTS login_risk_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    login_time DATETIME COMMENT '登录时间',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    device_id VARCHAR(100) COMMENT '设备ID',
    location VARCHAR(200) COMMENT '登录地点',
    risk_score INT DEFAULT 0 COMMENT '风险分数(0-100)',
    risk_level VARCHAR(20) COMMENT '风险等级(LOW/MEDIUM/HIGH)',
    assessment_reason TEXT COMMENT '评估原因',
    action_taken VARCHAR(20) COMMENT '采取动作(ALLOW/CHALLENGE/BLOCK)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time),
    INDEX idx_risk_level (risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录风险评估表';

CREATE TABLE IF NOT EXISTS conversation_context (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    conversation_id VARCHAR(100) NOT NULL COMMENT '会话ID',
    context_data TEXT COMMENT '上下文数据(JSON)',
    context_type VARCHAR(50) COMMENT '上下文类型',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话上下文表';

CREATE TABLE IF NOT EXISTS conversation_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    conversation_id VARCHAR(100) NOT NULL COMMENT '会话ID',
    summary TEXT COMMENT '会话摘要',
    keywords VARCHAR(500) COMMENT '关键词',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话摘要表';

CREATE TABLE IF NOT EXISTS chat_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    message_id BIGINT COMMENT '消息ID',
    image_url VARCHAR(500) COMMENT '图片URL',
    image_type VARCHAR(50) COMMENT '图片类型',
    width INT COMMENT '宽度',
    height INT COMMENT '高度',
    size INT COMMENT '大小(字节)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_message_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天图片表';

CREATE TABLE IF NOT EXISTS chat_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    message_id BIGINT COMMENT '消息ID',
    file_name VARCHAR(200) COMMENT '文件名',
    file_path VARCHAR(500) COMMENT '文件路径',
    file_type VARCHAR(50) COMMENT '文件类型',
    file_size INT COMMENT '文件大小(字节)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_message_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天文件表';

CREATE TABLE IF NOT EXISTS ai_system (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    system_key VARCHAR(100) NOT NULL UNIQUE COMMENT '系统配置键',
    system_value TEXT COMMENT '系统配置值',
    description VARCHAR(200) COMMENT '配置描述',
    category VARCHAR(50) COMMENT '配置分类',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_system_key (system_key),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

CREATE TABLE IF NOT EXISTS user_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_chats INT DEFAULT 0 COMMENT '总聊天次数',
    total_messages INT DEFAULT 0 COMMENT '总消息数',
    total_tokens INT DEFAULT 0 COMMENT '总token数',
    total_time_spent INT DEFAULT 0 COMMENT '总耗时(分钟)',
    last_active_date DATE COMMENT '最后活跃日期',
    streak_days INT DEFAULT 0 COMMENT '连续活跃天数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户统计表';

CREATE TABLE IF NOT EXISTS user_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户标签表';

CREATE TABLE IF NOT EXISTS user_badge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    badge_name VARCHAR(50) NOT NULL COMMENT '徽章名称',
    badge_level INT DEFAULT 1 COMMENT '徽章等级',
    earned_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    INDEX idx_user_id (user_id),
    INDEX idx_badge_name (badge_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户徽章表';

CREATE TABLE IF NOT EXISTS user_interest (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    interest_name VARCHAR(50) NOT NULL COMMENT '兴趣名称',
    interest_level INT DEFAULT 1 COMMENT '兴趣程度',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_interest_name (interest_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户兴趣表';

CREATE TABLE IF NOT EXISTS user_view_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    view_type VARCHAR(50) COMMENT '浏览类型',
    view_id BIGINT COMMENT '浏览ID',
    view_count INT DEFAULT 1 COMMENT '浏览次数',
    last_view_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后浏览时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_view_type (view_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户浏览记录表';

CREATE TABLE IF NOT EXISTS social_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT COMMENT '用户ID',
    share_type VARCHAR(20) COMMENT '分享类型',
    share_count INT DEFAULT 0 COMMENT '分享次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社交分享表';

CREATE TABLE IF NOT EXISTS oauth_provider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    provider VARCHAR(50) NOT NULL COMMENT 'OAuth提供商',
    provider_id VARCHAR(100) NOT NULL COMMENT '提供商用户ID',
    provider_user_id VARCHAR(100) COMMENT '提供商用户ID(兼容)',
    access_token VARCHAR(500) COMMENT '访问令牌',
    refresh_token VARCHAR(500) COMMENT '刷新令牌',
    expire_time DATETIME COMMENT '令牌过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_provider (provider),
    UNIQUE KEY uk_provider_user (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth登录表';

CREATE TABLE IF NOT EXISTS login_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    alert_type VARCHAR(50) COMMENT '告警类型',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    location VARCHAR(100) COMMENT '地理位置',
    device_info VARCHAR(200) COMMENT '设备信息',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录告警表';

CREATE TABLE IF NOT EXISTS character_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    character_id BIGINT NOT NULL COMMENT '角色ID',
    memory_key VARCHAR(100) COMMENT '记忆键',
    memory_value TEXT COMMENT '记忆值',
    memory_type VARCHAR(50) COMMENT '记忆类型',
    importance INT DEFAULT 1 COMMENT '重要程度',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_character_id (character_id),
    INDEX idx_memory_key (memory_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色记忆表';

CREATE TABLE IF NOT EXISTS character_preference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    character_id BIGINT NOT NULL COMMENT '角色ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    preference_key VARCHAR(100) COMMENT '偏好键',
    preference_value VARCHAR(500) COMMENT '偏好值',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_character_id (character_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色偏好表';

CREATE TABLE IF NOT EXISTS character_interaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    character_id BIGINT NOT NULL COMMENT '角色ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    interaction_type VARCHAR(50) COMMENT '交互类型',
    interaction_count INT DEFAULT 0 COMMENT '交互次数',
    last_interaction_time DATETIME COMMENT '最后交互时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_character_id (character_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色交互表';

CREATE TABLE IF NOT EXISTS ai_elf_focus (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    elf_id BIGINT NOT NULL COMMENT '精灵ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    focus_type VARCHAR(50) COMMENT '专注类型',
    focus_start_time VARCHAR(20) COMMENT '开始时间',
    focus_end_time VARCHAR(20) COMMENT '结束时间',
    focus_duration INT DEFAULT 0 COMMENT '专注时长(分钟)',
    is_completed BOOLEAN DEFAULT FALSE COMMENT '是否完成',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (elf_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精灵专注表';

CREATE TABLE IF NOT EXISTS ai_elf_health_reminder (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    elf_id BIGINT NOT NULL COMMENT '精灵ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    reminder_type VARCHAR(50) COMMENT '提醒类型',
    reminder_message TEXT COMMENT '提醒消息',
    reminder_time VARCHAR(20) COMMENT '提醒时间',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (elf_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精灵健康提醒表';

CREATE TABLE IF NOT EXISTS elf_voice_command (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    elf_id BIGINT NOT NULL COMMENT '精灵ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    command_text VARCHAR(200) COMMENT '命令文本',
    command_type VARCHAR(50) COMMENT '命令类型',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (elf_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精灵语音命令表';

CREATE TABLE IF NOT EXISTS sleep_analysis_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    elf_id BIGINT NOT NULL COMMENT '精灵ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    report_date DATE COMMENT '报告日期',
    total_sleep_hours DOUBLE DEFAULT 0 COMMENT '总睡眠时长',
    deep_sleep_hours DOUBLE DEFAULT 0 COMMENT '深度睡眠时长',
    sleep_efficiency DOUBLE DEFAULT 0 COMMENT '睡眠效率',
    sleep_score INT DEFAULT 0 COMMENT '睡眠评分',
    suggestions TEXT COMMENT '改善建议',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (elf_id),
    INDEX idx_user_id (user_id),
    INDEX idx_report_date (report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='睡眠分析报告表';

CREATE TABLE IF NOT EXISTS health_analysis_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    elf_id BIGINT NOT NULL COMMENT '精灵ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    analysis_type VARCHAR(50) COMMENT '分析类型',
    analysis_data TEXT COMMENT '分析数据(JSON)',
    analysis_score INT DEFAULT 0 COMMENT '分析评分',
    recommendations TEXT COMMENT '建议',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_elf_id (elf_id),
    INDEX idx_user_id (user_id),
    INDEX idx_analysis_type (analysis_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康分析记录表';

CREATE TABLE IF NOT EXISTS group_bot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    group_id BIGINT NOT NULL COMMENT '群ID',
    bot_name VARCHAR(100) COMMENT '机器人名称',
    bot_type VARCHAR(50) COMMENT '机器人类型',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群机器人表';

CREATE TABLE IF NOT EXISTS group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    group_id BIGINT NOT NULL COMMENT '群ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) DEFAULT 'member' COMMENT '角色',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    leave_time DATETIME COMMENT '离开时间',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否活跃',
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表(冗余)';

CREATE TABLE IF NOT EXISTS group_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    group_id BIGINT NOT NULL COMMENT '群ID',
    user_id BIGINT COMMENT '用户ID',
    content TEXT COMMENT '消息内容',
    message_type VARCHAR(20) DEFAULT 'text' COMMENT '消息类型',
    reply_to_message_id BIGINT COMMENT '回复消息ID',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息表(冗余)';
