-- 用户认证模块改进
CREATE TABLE IF NOT EXISTS oauth_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    access_token VARCHAR(500),
    refresh_token VARCHAR(500),
    token_expire_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_provider_user (provider, provider_user_id)
);

CREATE TABLE IF NOT EXISTS login_risk_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    login_time DATETIME,
    ip_address VARCHAR(50),
    device_id VARCHAR(100),
    location VARCHAR(100),
    risk_score INT DEFAULT 0,
    risk_level VARCHAR(20) DEFAULT 'LOW',
    assessment_reason VARCHAR(500),
    action_taken VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- AI虚拟角色模块改进
CREATE TABLE IF NOT EXISTS character_avatar_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    category VARCHAR(50),
    style VARCHAR(50),
    image_url VARCHAR(500),
    is_system BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS character_emotion_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_id BIGINT,
    user_id BIGINT,
    emotion_type VARCHAR(50),
    emotion_intensity DOUBLE,
    trigger_message TEXT,
    response_text TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- AI对话模块改进
CREATE TABLE IF NOT EXISTS chat_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100),
    user_id BIGINT,
    character_id BIGINT,
    image_url VARCHAR(500),
    ocr_result TEXT,
    image_description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100),
    user_id BIGINT,
    character_id BIGINT,
    file_name VARCHAR(200),
    file_url VARCHAR(500),
    file_type VARCHAR(50),
    file_size BIGINT,
    extracted_content TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conversation_context_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100),
    compressed_context TEXT,
    key_points TEXT,
    topic_summary TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 社交功能模块改进
ALTER TABLE group_chat ADD COLUMN announcement TEXT;
ALTER TABLE group_chat ADD COLUMN settings JSON;
ALTER TABLE group_chat ADD COLUMN activity_score INT DEFAULT 0;
ALTER TABLE group_chat ADD COLUMN last_activity_time DATETIME;

CREATE TABLE IF NOT EXISTS group_bot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT,
    bot_name VARCHAR(100),
    bot_description VARCHAR(500),
    enabled BOOLEAN DEFAULT TRUE,
    trigger_keywords TEXT,
    auto_reply_rules JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS group_bot_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bot_id BIGINT,
    group_id BIGINT,
    message_content TEXT,
    trigger_type VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS social_recommendation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    recommended_user_id BIGINT,
    recommendation_type VARCHAR(50),
    similarity_score DOUBLE,
    common_interests TEXT,
    common_friends INT DEFAULT 0,
    interaction_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS community_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    community_id BIGINT,
    activity_name VARCHAR(200),
    activity_description TEXT,
    start_time DATETIME,
    end_time DATETIME,
    organizer_id BIGINT,
    participant_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'UPCOMING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS community_activity_participant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT,
    user_id BIGINT,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- AI小精灵模块改进
ALTER TABLE ai_elf ADD COLUMN voice_pack VARCHAR(100);
ALTER TABLE ai_elf ADD COLUMN voice_enabled BOOLEAN DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS elf_voice_command (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    elf_id BIGINT,
    user_id BIGINT,
    command_text VARCHAR(200),
    command_type VARCHAR(50),
    executed BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS health_analysis_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    elf_id BIGINT,
    user_id BIGINT,
    analysis_date DATE,
    focus_duration INT DEFAULT 0,
    sleep_quality INT DEFAULT 0,
    step_count INT DEFAULT 0,
    mood_score INT DEFAULT 0,
    health_score INT DEFAULT 0,
    suggestions TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 内容推荐与搜索模块改进
CREATE TABLE IF NOT EXISTS user_content_preference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    content_type VARCHAR(50),
    preference_score DOUBLE DEFAULT 0,
    interaction_count INT DEFAULT 0,
    last_interaction_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS content_feature (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT,
    feature_type VARCHAR(50),
    feature_value VARCHAR(200),
    weight DOUBLE DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS search_query_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    query_text VARCHAR(500),
    query_type VARCHAR(50),
    result_count INT DEFAULT 0,
    clicked_result_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 数据池与AI系统管理模块改进
CREATE TABLE IF NOT EXISTS knowledge_graph_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_type VARCHAR(50),
    node_name VARCHAR(200),
    node_description TEXT,
    properties JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_graph_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_node_id BIGINT,
    target_node_id BIGINT,
    relation_type VARCHAR(50),
    relation_description VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS model_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT,
    version_number VARCHAR(50),
    version_description TEXT,
    trained_data_size BIGINT,
    accuracy DOUBLE,
    deployed BOOLEAN DEFAULT FALSE,
    deployed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS api_call_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100),
    endpoint VARCHAR(200),
    call_count BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    avg_response_time DOUBLE DEFAULT 0,
    peak_response_time DOUBLE DEFAULT 0,
    min_response_time DOUBLE DEFAULT 0,
    statistics_date DATE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 管理员后台模块改进
CREATE TABLE IF NOT EXISTS system_monitor_metric (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(100),
    metric_value DOUBLE,
    metric_unit VARCHAR(50),
    collection_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_profile_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    profile_type VARCHAR(50),
    interest_tags TEXT,
    behavior_pattern TEXT,
    engagement_level VARCHAR(20),
    risk_score INT DEFAULT 0,
    analysis_date DATE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS content_moderation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT,
    content_type VARCHAR(50),
    content_text TEXT,
    moderation_score DOUBLE,
    moderation_result VARCHAR(20),
    violation_types TEXT,
    ai_detection BOOLEAN DEFAULT TRUE,
    human_review BOOLEAN DEFAULT FALSE,
    reviewed_by BIGINT,
    reviewed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 索引优化
CREATE INDEX idx_oauth_provider ON oauth_user(provider, provider_user_id);
CREATE INDEX idx_login_risk_user ON login_risk_assessment(user_id);
CREATE INDEX idx_char_emotion_char ON character_emotion_log(character_id);
CREATE INDEX idx_chat_image_session ON chat_image(session_id);
CREATE INDEX idx_chat_file_session ON chat_file(session_id);
CREATE INDEX idx_context_summary_session ON conversation_context_summary(session_id);
CREATE INDEX idx_group_bot_group ON group_bot(group_id);
CREATE INDEX idx_social_rec_user ON social_recommendation(user_id);
CREATE INDEX idx_community_activity_comm ON community_activity(community_id);
CREATE INDEX idx_health_record_user ON health_analysis_record(user_id);
CREATE INDEX idx_content_feature_post ON content_feature(post_id);
CREATE INDEX idx_search_query_user ON search_query_log(user_id);
CREATE INDEX idx_model_version_model ON model_version(model_id);
CREATE INDEX idx_api_stats_service ON api_call_statistics(service_name, statistics_date);
CREATE INDEX idx_sys_monitor_time ON system_monitor_metric(collection_time);
CREATE INDEX idx_user_profile_user ON user_profile_analysis(user_id);
CREATE INDEX idx_content_mod_content ON content_moderation(content_id, content_type);
