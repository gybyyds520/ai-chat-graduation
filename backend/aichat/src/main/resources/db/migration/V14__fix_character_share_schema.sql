-- V14: Fix character_share table schema
--
-- Root cause: Entity/Mapper-DB schema mismatch causing "数据访问异常" (HTTP 500)
-- when fetching characters by userId via /api/character/list/user/{userId}.
--
-- Code (CharacterShare entity + CharacterShareMapper SQL) expects:
--   shared_by_user_id, shared_to_user_id, status, created_at, updated_at
-- But the legacy table (from V1) has:
--   from_user_id, to_user_id, shared_at  (and no status column)
--
-- Discovered during login E2E test: ChatView calls this endpoint on mount
-- and shows empty character list when it fails.

-- 1. Rename from_user_id -> shared_by_user_id (匹配实体 @TableField)
ALTER TABLE `character_share` CHANGE COLUMN `from_user_id` `shared_by_user_id` BIGINT NULL COMMENT '分享者用户ID';

-- 2. Rename to_user_id -> shared_to_user_id (匹配实体 @TableField)
ALTER TABLE `character_share` CHANGE COLUMN `to_user_id` `shared_to_user_id` BIGINT NULL COMMENT '被分享者用户ID';

-- 3. Add status column (Mapper SQL 过滤 status = 'ACTIVE')
ALTER TABLE `character_share` ADD COLUMN `status` VARCHAR(20) NULL DEFAULT 'ACTIVE' COMMENT '分享状态: ACTIVE/CANCELLED' AFTER `permissions`;

-- 4. Rename shared_at -> created_at (匹配实体 createdAt)
ALTER TABLE `character_share` CHANGE COLUMN `shared_at` `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 5. Add updated_at column (匹配实体 updatedAt)
ALTER TABLE `character_share` ADD COLUMN `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;

-- 6. 补充索引以匹配 character_isolation_migration.sql 的设计
CREATE INDEX `idx_shared_to_user_id` ON `character_share`(`shared_to_user_id`);
CREATE INDEX `idx_shared_by_user_id` ON `character_share`(`shared_by_user_id`);
