-- V11: Fix ai_elf table schema - add columns missing from entity mapping
--
-- Root cause: AiElf entity has personality, focusCount, totalFocusTime, healthPoints
-- fields but the ai_elf table didn't have these columns. INSERT failed with
-- DataAccessException (500 error) when creating a new elf.
--
-- Discovered during E2E testing (Phase 3 - AI Elf create returned 500).

ALTER TABLE ai_elf ADD COLUMN personality VARCHAR(255) NULL COMMENT 'AI personality' AFTER theme;
ALTER TABLE ai_elf ADD COLUMN focus_count INT DEFAULT 0 COMMENT 'focus count' AFTER experience;
ALTER TABLE ai_elf ADD COLUMN total_focus_time INT DEFAULT 0 COMMENT 'total focus time (minutes)' AFTER focus_count;
ALTER TABLE ai_elf ADD COLUMN health_points INT DEFAULT 100 COMMENT 'health points' AFTER energy;
