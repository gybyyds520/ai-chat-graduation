-- V15: Fix friend_chat_message and user_follow table schemas
--
-- 问题1: friend_chat_message 表缺少实体类定义的多个列
--   导致 INSERT 时 "Unknown column 'message_type' in 'field list'"，
--   好友聊天消息无法持久化，刷新页面后消息全部丢失。
--
-- 问题2: user_follow 表列名与实体/Mapper 不一致
--   实体用 follower_id/following_id/is_mutual，表里是 user_id/follow_id，
--   导致 "Unknown column 'uf.follower_id' in 'where clause'" 500 错误。

-- ========== 1. friend_chat_message 表：补齐缺失列 ==========
ALTER TABLE `friend_chat_message` ADD COLUMN `message_type` VARCHAR(50) NULL DEFAULT 'text' COMMENT '消息类型: text/file/image' AFTER `content`;
ALTER TABLE `friend_chat_message` ADD COLUMN `is_read` TINYINT(1) NULL DEFAULT 0 COMMENT '是否已读' AFTER `type`;
ALTER TABLE `friend_chat_message` ADD COLUMN `read_at` DATETIME NULL COMMENT '已读时间' AFTER `is_read`;
ALTER TABLE `friend_chat_message` ADD COLUMN `file_name` VARCHAR(255) NULL COMMENT '文件名' AFTER `read_at`;
ALTER TABLE `friend_chat_message` ADD COLUMN `file_path` VARCHAR(500) NULL COMMENT '文件路径' AFTER `file_name`;
ALTER TABLE `friend_chat_message` ADD COLUMN `file_size` BIGINT NULL COMMENT '文件大小(字节)' AFTER `file_path`;
ALTER TABLE `friend_chat_message` ADD COLUMN `extra_data` TEXT NULL COMMENT '额外数据(JSON)' AFTER `file_size`;
ALTER TABLE `friend_chat_message` ADD COLUMN `sender_name` VARCHAR(100) NULL COMMENT '发送者名称' AFTER `sender_id`;
ALTER TABLE `friend_chat_message` ADD COLUMN `receiver_name` VARCHAR(100) NULL COMMENT '接收者名称' AFTER `receiver_id`;

-- ========== 2. user_follow 表：重命名列对齐实体 ==========
-- 实体: followerId -> follower_id, followingId -> following_id, isMutual -> is_mutual
ALTER TABLE `user_follow` CHANGE COLUMN `user_id` `follower_id` BIGINT NOT NULL COMMENT '关注者用户ID';
ALTER TABLE `user_follow` CHANGE COLUMN `follow_id` `following_id` BIGINT NOT NULL COMMENT '被关注者用户ID';
ALTER TABLE `user_follow` ADD COLUMN `is_mutual` TINYINT(1) NULL DEFAULT 0 COMMENT '是否互相关注' AFTER `following_id`;
