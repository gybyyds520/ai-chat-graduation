-- V13: Fix conversation_context table schema
--
-- Root cause: Entity-DB schema mismatch causing "服务器内部错误" during SSE chat.
-- ConversationContext entity inserts (session_id, user_id, character_id, context_key,
-- context_value, expire_time, created_at) but the legacy V1 column `conversation_id`
-- is still NOT NULL without a default value, so the INSERT fails with:
--   java.sql.SQLException: Field 'conversation_id' doesn't have a default value
--
-- The entity no longer references conversation_id (it uses session_id instead, which
-- already exists in the live DB). Making the legacy column nullable is the minimal
-- safe fix — it does not touch any other table or column, and existing rows are
-- unaffected (NULL is allowed going forward).
--
-- Discovered during E2E testing (SSE stream returned error before the AI model was called).

ALTER TABLE `conversation_context` MODIFY COLUMN `conversation_id` VARCHAR(100) NULL COMMENT '会话ID(遗留列,实体已改用session_id)';
