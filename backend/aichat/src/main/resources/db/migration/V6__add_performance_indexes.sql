-- =====================================================================
-- V6: 性能索引补齐 - Phase 3 (P3-数据库优化) 引入
-- =====================================================================
-- 目的：针对高频查询场景补充联合索引，避免全表扫描。
--
-- 查询场景分析：
--   1. 时间线分页：SELECT * FROM social_post WHERE user_id=? AND is_deleted=0 ORDER BY create_time DESC LIMIT 20
--   2. 会话消息：SELECT * FROM chat_message WHERE conversation_id=? AND is_deleted=0 ORDER BY create_time ASC LIMIT 50
--   3. 好友关系：SELECT * FROM friendship WHERE (user_id=? OR friend_id=?) AND status='ACCEPTED'
--   4. 用户登录：SELECT * FROM user WHERE username=? AND is_deleted=0
--   5. 评论查询：SELECT * FROM social_comment WHERE post_id=? AND is_deleted=0 ORDER BY create_time ASC
--   6. 群成员查询：SELECT * FROM group_chat_member WHERE group_id=? AND is_deleted=0
-- =====================================================================

-- 用户表：登录查询高频字段
CREATE INDEX IF NOT EXISTS `idx_user_username_deleted` ON `user` (`username`, `is_deleted`);
CREATE INDEX IF NOT EXISTS `idx_user_email_deleted` ON `user` (`email`, `is_deleted`);

-- 聊天消息表：按会话拉取历史消息
CREATE INDEX IF NOT EXISTS `idx_chat_msg_conv_deleted_time` ON `chat_message` (`conversation_id`, `is_deleted`, `create_time`);

-- 对话表：用户会话列表
CREATE INDEX IF NOT EXISTS `idx_conv_user_deleted_time` ON `conversation` (`user_id`, `is_deleted`, `create_time`);

-- 社交动态表：时间线分页查询
CREATE INDEX IF NOT EXISTS `idx_post_user_deleted_time` ON `social_post` (`user_id`, `is_deleted`, `create_time`);
CREATE INDEX IF NOT EXISTS `idx_post_deleted_time` ON `social_post` (`is_deleted`, `create_time`);

-- 社交评论表：按动态拉取评论列表
CREATE INDEX IF NOT EXISTS `idx_comment_post_deleted_time` ON `social_comment` (`post_id`, `is_deleted`, `create_time`);

-- 社交点赞表：查询是否已点赞
CREATE INDEX IF NOT EXISTS `idx_like_user_target` ON `social_like` (`user_id`, `target_type`, `target_id`);

-- 好友关系表：双向查询好友列表
CREATE INDEX IF NOT EXISTS `idx_friendship_user_status` ON `friendship` (`user_id`, `status`);
CREATE INDEX IF NOT EXISTS `idx_friendship_friend_status` ON `friendship` (`friend_id`, `status`);

-- 群聊成员表：查询群成员
CREATE INDEX IF NOT EXISTS `idx_group_member_group_deleted` ON `group_chat_member` (`group_id`, `is_deleted`);

-- 好友聊天消息表：按会话拉取历史
CREATE INDEX IF NOT EXISTS `idx_friend_msg_session_deleted_time` ON `friend_chat_message` (`session_id`, `is_deleted`, `create_time`);

-- 支付订单表：按用户与状态查询
CREATE INDEX IF NOT EXISTS `idx_payment_user_status_time` ON `payment` (`user_id`, `status`, `create_time`);

-- 访问日志表：按时间与操作查询
CREATE INDEX IF NOT EXISTS `idx_access_log_user_time` ON `access_log` (`user_id`, `create_time`);
CREATE INDEX IF NOT EXISTS `idx_access_log_action_time` ON `access_log` (`action`, `create_time`);
