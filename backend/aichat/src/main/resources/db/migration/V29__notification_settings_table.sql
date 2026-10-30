-- V29: 用户级通知设置表
-- 存储每个用户是否接收 like / comment / share / system 四类通知的开关。
-- 幂等：CREATE TABLE IF NOT EXISTS，测试环境 Flyway 禁用不受影响。

CREATE TABLE IF NOT EXISTS notification_setting (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  like_enabled TINYINT(1) DEFAULT 1,
  comment_enabled TINYINT(1) DEFAULT 1,
  share_enabled TINYINT(1) DEFAULT 1,
  system_enabled TINYINT(1) DEFAULT 1,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
