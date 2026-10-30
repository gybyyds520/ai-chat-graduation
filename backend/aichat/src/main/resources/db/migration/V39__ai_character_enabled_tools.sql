-- M2-A4: 角色启用工具列表（逗号分隔工具名；为空表示全部可用）
ALTER TABLE ai_character ADD COLUMN enabled_tools VARCHAR(255) DEFAULT NULL COMMENT '角色启用的工具名，逗号分隔' AFTER voice_id;
