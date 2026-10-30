-- V12: Fix character_memory and character_interaction table schema
--
-- Root cause: Entity-DB schema mismatch causing "服务器内部错误" during SSE chat.
-- CharacterMemory entity has userId, lastAccessed fields not in DB table.
-- CharacterInteraction entity has totalMessages, firstInteractionAt, lastInteractionAt
-- columns not in DB table (DB had last_interaction_time instead).
--
-- Discovered during E2E testing (Phase 5 - SSE stream returned error).

-- Fix character_memory: add user_id and last_accessed columns
ALTER TABLE character_memory ADD COLUMN user_id BIGINT NULL COMMENT 'user id' AFTER character_id;
ALTER TABLE character_memory ADD COLUMN last_accessed DATETIME NULL COMMENT 'last accessed time' AFTER importance;

-- Fix character_interaction: add missing columns
ALTER TABLE character_interaction ADD COLUMN total_messages INT DEFAULT 0 COMMENT 'total messages' AFTER interaction_count;
ALTER TABLE character_interaction ADD COLUMN first_interaction_at DATETIME NULL COMMENT 'first interaction time' AFTER total_messages;
ALTER TABLE character_interaction ADD COLUMN last_interaction_at DATETIME NULL COMMENT 'last interaction at' AFTER first_interaction_at;
