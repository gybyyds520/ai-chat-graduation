-- V10: Fix social_like table schema - add target_type and target_id columns
--
-- Root cause: SocialLike entity and SocialLikeMapper use target_type/target_id columns
-- to support liking both posts and comments. But the social_like table only had post_id,
-- causing INSERT/SELECT failures (500 error) when toggleLike() was called.
--
-- Fix: Add target_type (default 'post') and target_id columns, migrate existing data
-- from post_id, and add an index for the common query pattern.
--
-- Discovered during E2E testing (Phase 2 - Social like flow returned 500).

-- Add target_type column (defaults to 'post' for existing rows)
ALTER TABLE social_like ADD COLUMN target_type VARCHAR(20) NOT NULL DEFAULT 'post' COMMENT 'target type: post/comment' AFTER id;

-- Add target_id column
ALTER TABLE social_like ADD COLUMN target_id BIGINT NOT NULL DEFAULT 0 COMMENT 'target id' AFTER target_type;

-- Migrate existing data from post_id to target_id
UPDATE social_like SET target_type = 'post', target_id = post_id WHERE post_id IS NOT NULL;

-- Make post_id nullable (deprecated: entity now uses target_id, post_id left for legacy compat)
ALTER TABLE social_like MODIFY COLUMN post_id BIGINT NULL DEFAULT NULL COMMENT 'deprecated: use target_id instead';

-- Add composite index for the findLike/countLikes query pattern
CREATE INDEX idx_social_like_target ON social_like(target_type, target_id);
CREATE INDEX idx_social_like_user_target ON social_like(target_type, target_id, user_id);
