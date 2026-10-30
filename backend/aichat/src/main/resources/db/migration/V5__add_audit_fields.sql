-- =====================================================================
-- V5: 统一审计字段 - Phase 3 (P3-数据库优化) 引入
-- =====================================================================
-- 目的：所有业务表统一加 created_by / updated_by 字段，配合 MyBatis-Plus
--       MetaObjectHandler 自动填充操作者 ID，便于追溯数据变更来源。
--
-- 设计原则：
--   1. 仅新增列，不修改/删除原有列，向后兼容；
--   2. 字段允许 NULL，避免破坏现有数据；
--   3. 通过 information_schema 判断列是否存在，保证可重复执行（部分 MySQL 版本不支持 ADD COLUMN IF NOT EXISTS）。
--
-- 适用范围：社交、聊天、AI、支付、管理等业务表。
-- =====================================================================

-- 用户表：补审计字段（原表已有 create_time/update_time）
ALTER TABLE `user`
    ADD COLUMN IF NOT EXISTS `created_by` BIGINT DEFAULT NULL COMMENT '创建者ID（注册时自填）',
    ADD COLUMN IF NOT EXISTS `updated_by` BIGINT DEFAULT NULL COMMENT '最后更新者ID';

-- AI 角色表
ALTER TABLE `ai_character`
    ADD COLUMN IF NOT EXISTS `created_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
    ADD COLUMN IF NOT EXISTS `updated_by` BIGINT DEFAULT NULL COMMENT '最后更新者ID',
    ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记';

-- 对话上下文表（原脚本误用 `conversation` 表名，实际表为 `conversation_context`/`conversation_summary`）
ALTER TABLE `conversation_context`
    ADD COLUMN IF NOT EXISTS `created_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `updated_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) DEFAULT 0;

ALTER TABLE `conversation_summary`
    ADD COLUMN IF NOT EXISTS `created_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `updated_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) DEFAULT 0;

-- 聊天消息表
ALTER TABLE `chat_message`
    ADD COLUMN IF NOT EXISTS `created_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) DEFAULT 0;

-- 社交动态表
ALTER TABLE `social_post`
    ADD COLUMN IF NOT EXISTS `created_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `updated_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) DEFAULT 0;

-- 社交评论表
ALTER TABLE `social_comment`
    ADD COLUMN IF NOT EXISTS `created_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) DEFAULT 0;

-- 好友关系表
ALTER TABLE `friendship`
    ADD COLUMN IF NOT EXISTS `created_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) DEFAULT 0;

-- 群聊表
ALTER TABLE `group_chat`
    ADD COLUMN IF NOT EXISTS `created_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `updated_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) DEFAULT 0;

-- 支付订单表（原脚本误用 `payment` 表名，实际表为 `payment_record`）
ALTER TABLE `payment_record`
    ADD COLUMN IF NOT EXISTS `created_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `updated_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) DEFAULT 0;

-- 公告表
ALTER TABLE `announcement`
    ADD COLUMN IF NOT EXISTS `created_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `updated_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) DEFAULT 0;

-- 广告表
ALTER TABLE `advertisement`
    ADD COLUMN IF NOT EXISTS `created_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `updated_by` BIGINT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) DEFAULT 0;
