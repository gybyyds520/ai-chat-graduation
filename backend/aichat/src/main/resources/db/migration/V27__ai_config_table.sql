-- ============================================================
-- V27: AI 全局配置表 ai_config
-- 1. ai_config：键值对持久化 defaultModel/temperature/maxTokens/
--    requestTimeoutSec/aiChatEnabled/maintenanceNotice
-- 幂等：表/行已存在则跳过；配置主键冲突则忽略。
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(100) NOT NULL UNIQUE,
  config_value TEXT,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO ai_config (config_key, config_value) VALUES
  ('defaultModel', 'llama3.2:3b'),
  ('temperature', '0.7'),
  ('maxTokens', '2048'),
  ('requestTimeoutSec', '60'),
  ('aiChatEnabled', 'true'),
  ('maintenanceNotice', '');
