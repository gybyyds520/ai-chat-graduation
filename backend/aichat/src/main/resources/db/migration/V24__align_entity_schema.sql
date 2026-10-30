-- ============================================================
-- V24: 实体与真实表结构对齐（补齐历史迁移遗漏列，幂等）
-- 由实体-表结构差异扫描自动生成，可重复执行
-- ============================================================

SET @has_ai_elf_alarm_time := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_alarm' AND COLUMN_NAME = 'time');
SET @ddl_ai_elf_alarm_time := IF(@has_ai_elf_alarm_time = 0, 'ALTER TABLE `ai_elf_alarm` ADD COLUMN `time` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_ai_elf_alarm_time FROM @ddl_ai_elf_alarm_time;
EXECUTE s_ai_elf_alarm_time;
DEALLOCATE PREPARE s_ai_elf_alarm_time;

SET @has_ai_elf_alarm_enabled := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_alarm' AND COLUMN_NAME = 'enabled');
SET @ddl_ai_elf_alarm_enabled := IF(@has_ai_elf_alarm_enabled = 0, 'ALTER TABLE `ai_elf_alarm` ADD COLUMN `enabled` TINYINT(1) NULL', 'SELECT 1');
PREPARE s_ai_elf_alarm_enabled FROM @ddl_ai_elf_alarm_enabled;
EXECUTE s_ai_elf_alarm_enabled;
DEALLOCATE PREPARE s_ai_elf_alarm_enabled;

SET @has_ai_elf_alarm_ringtone := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_alarm' AND COLUMN_NAME = 'ringtone');
SET @ddl_ai_elf_alarm_ringtone := IF(@has_ai_elf_alarm_ringtone = 0, 'ALTER TABLE `ai_elf_alarm` ADD COLUMN `ringtone` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_ai_elf_alarm_ringtone FROM @ddl_ai_elf_alarm_ringtone;
EXECUTE s_ai_elf_alarm_ringtone;
DEALLOCATE PREPARE s_ai_elf_alarm_ringtone;

SET @has_ai_elf_alarm_message := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_alarm' AND COLUMN_NAME = 'message');
SET @ddl_ai_elf_alarm_message := IF(@has_ai_elf_alarm_message = 0, 'ALTER TABLE `ai_elf_alarm` ADD COLUMN `message` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_ai_elf_alarm_message FROM @ddl_ai_elf_alarm_message;
EXECUTE s_ai_elf_alarm_message;
DEALLOCATE PREPARE s_ai_elf_alarm_message;

SET @has_ai_elf_alarm_smart_enabled := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_alarm' AND COLUMN_NAME = 'smart_enabled');
SET @ddl_ai_elf_alarm_smart_enabled := IF(@has_ai_elf_alarm_smart_enabled = 0, 'ALTER TABLE `ai_elf_alarm` ADD COLUMN `smart_enabled` TINYINT(1) NULL', 'SELECT 1');
PREPARE s_ai_elf_alarm_smart_enabled FROM @ddl_ai_elf_alarm_smart_enabled;
EXECUTE s_ai_elf_alarm_smart_enabled;
DEALLOCATE PREPARE s_ai_elf_alarm_smart_enabled;

SET @has_ai_elf_alarm_adjusted_minutes := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_alarm' AND COLUMN_NAME = 'adjusted_minutes');
SET @ddl_ai_elf_alarm_adjusted_minutes := IF(@has_ai_elf_alarm_adjusted_minutes = 0, 'ALTER TABLE `ai_elf_alarm` ADD COLUMN `adjusted_minutes` INT NULL', 'SELECT 1');
PREPARE s_ai_elf_alarm_adjusted_minutes FROM @ddl_ai_elf_alarm_adjusted_minutes;
EXECUTE s_ai_elf_alarm_adjusted_minutes;
DEALLOCATE PREPARE s_ai_elf_alarm_adjusted_minutes;

SET @has_ai_elf_alarm_updated_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_alarm' AND COLUMN_NAME = 'updated_at');
SET @ddl_ai_elf_alarm_updated_at := IF(@has_ai_elf_alarm_updated_at = 0, 'ALTER TABLE `ai_elf_alarm` ADD COLUMN `updated_at` DATETIME NULL', 'SELECT 1');
PREPARE s_ai_elf_alarm_updated_at FROM @ddl_ai_elf_alarm_updated_at;
EXECUTE s_ai_elf_alarm_updated_at;
DEALLOCATE PREPARE s_ai_elf_alarm_updated_at;

SET @has_ai_elf_focus_start_time := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_focus' AND COLUMN_NAME = 'start_time');
SET @ddl_ai_elf_focus_start_time := IF(@has_ai_elf_focus_start_time = 0, 'ALTER TABLE `ai_elf_focus` ADD COLUMN `start_time` DATETIME NULL', 'SELECT 1');
PREPARE s_ai_elf_focus_start_time FROM @ddl_ai_elf_focus_start_time;
EXECUTE s_ai_elf_focus_start_time;
DEALLOCATE PREPARE s_ai_elf_focus_start_time;

SET @has_ai_elf_focus_end_time := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_focus' AND COLUMN_NAME = 'end_time');
SET @ddl_ai_elf_focus_end_time := IF(@has_ai_elf_focus_end_time = 0, 'ALTER TABLE `ai_elf_focus` ADD COLUMN `end_time` DATETIME NULL', 'SELECT 1');
PREPARE s_ai_elf_focus_end_time FROM @ddl_ai_elf_focus_end_time;
EXECUTE s_ai_elf_focus_end_time;
DEALLOCATE PREPARE s_ai_elf_focus_end_time;

SET @has_ai_elf_focus_duration_minutes := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_focus' AND COLUMN_NAME = 'duration_minutes');
SET @ddl_ai_elf_focus_duration_minutes := IF(@has_ai_elf_focus_duration_minutes = 0, 'ALTER TABLE `ai_elf_focus` ADD COLUMN `duration_minutes` INT NULL', 'SELECT 1');
PREPARE s_ai_elf_focus_duration_minutes FROM @ddl_ai_elf_focus_duration_minutes;
EXECUTE s_ai_elf_focus_duration_minutes;
DEALLOCATE PREPARE s_ai_elf_focus_duration_minutes;

SET @has_ai_elf_focus_goal := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_focus' AND COLUMN_NAME = 'goal');
SET @ddl_ai_elf_focus_goal := IF(@has_ai_elf_focus_goal = 0, 'ALTER TABLE `ai_elf_focus` ADD COLUMN `goal` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_ai_elf_focus_goal FROM @ddl_ai_elf_focus_goal;
EXECUTE s_ai_elf_focus_goal;
DEALLOCATE PREPARE s_ai_elf_focus_goal;

SET @has_ai_elf_focus_completed := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_focus' AND COLUMN_NAME = 'completed');
SET @ddl_ai_elf_focus_completed := IF(@has_ai_elf_focus_completed = 0, 'ALTER TABLE `ai_elf_focus` ADD COLUMN `completed` TINYINT(1) NULL', 'SELECT 1');
PREPARE s_ai_elf_focus_completed FROM @ddl_ai_elf_focus_completed;
EXECUTE s_ai_elf_focus_completed;
DEALLOCATE PREPARE s_ai_elf_focus_completed;

SET @has_ai_elf_health_reminder_last_reminded := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_health_reminder' AND COLUMN_NAME = 'last_reminded');
SET @ddl_ai_elf_health_reminder_last_reminded := IF(@has_ai_elf_health_reminder_last_reminded = 0, 'ALTER TABLE `ai_elf_health_reminder` ADD COLUMN `last_reminded` DATETIME NULL', 'SELECT 1');
PREPARE s_ai_elf_health_reminder_last_reminded FROM @ddl_ai_elf_health_reminder_last_reminded;
EXECUTE s_ai_elf_health_reminder_last_reminded;
DEALLOCATE PREPARE s_ai_elf_health_reminder_last_reminded;

SET @has_ai_elf_health_reminder_enabled := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_health_reminder' AND COLUMN_NAME = 'enabled');
SET @ddl_ai_elf_health_reminder_enabled := IF(@has_ai_elf_health_reminder_enabled = 0, 'ALTER TABLE `ai_elf_health_reminder` ADD COLUMN `enabled` TINYINT(1) NULL', 'SELECT 1');
PREPARE s_ai_elf_health_reminder_enabled FROM @ddl_ai_elf_health_reminder_enabled;
EXECUTE s_ai_elf_health_reminder_enabled;
DEALLOCATE PREPARE s_ai_elf_health_reminder_enabled;

SET @has_ai_elf_health_reminder_interval_minutes := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_health_reminder' AND COLUMN_NAME = 'interval_minutes');
SET @ddl_ai_elf_health_reminder_interval_minutes := IF(@has_ai_elf_health_reminder_interval_minutes = 0, 'ALTER TABLE `ai_elf_health_reminder` ADD COLUMN `interval_minutes` INT NULL', 'SELECT 1');
PREPARE s_ai_elf_health_reminder_interval_minutes FROM @ddl_ai_elf_health_reminder_interval_minutes;
EXECUTE s_ai_elf_health_reminder_interval_minutes;
DEALLOCATE PREPARE s_ai_elf_health_reminder_interval_minutes;

SET @has_ai_elf_schedule_location := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_schedule' AND COLUMN_NAME = 'location');
SET @ddl_ai_elf_schedule_location := IF(@has_ai_elf_schedule_location = 0, 'ALTER TABLE `ai_elf_schedule` ADD COLUMN `location` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_ai_elf_schedule_location FROM @ddl_ai_elf_schedule_location;
EXECUTE s_ai_elf_schedule_location;
DEALLOCATE PREPARE s_ai_elf_schedule_location;

SET @has_ai_elf_schedule_description := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_schedule' AND COLUMN_NAME = 'description');
SET @ddl_ai_elf_schedule_description := IF(@has_ai_elf_schedule_description = 0, 'ALTER TABLE `ai_elf_schedule` ADD COLUMN `description` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_ai_elf_schedule_description FROM @ddl_ai_elf_schedule_description;
EXECUTE s_ai_elf_schedule_description;
DEALLOCATE PREPARE s_ai_elf_schedule_description;

SET @has_ai_elf_schedule_reminder_minutes := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_schedule' AND COLUMN_NAME = 'reminder_minutes');
SET @ddl_ai_elf_schedule_reminder_minutes := IF(@has_ai_elf_schedule_reminder_minutes = 0, 'ALTER TABLE `ai_elf_schedule` ADD COLUMN `reminder_minutes` INT NULL', 'SELECT 1');
PREPARE s_ai_elf_schedule_reminder_minutes FROM @ddl_ai_elf_schedule_reminder_minutes;
EXECUTE s_ai_elf_schedule_reminder_minutes;
DEALLOCATE PREPARE s_ai_elf_schedule_reminder_minutes;

SET @has_ai_elf_schedule_completed := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_schedule' AND COLUMN_NAME = 'completed');
SET @ddl_ai_elf_schedule_completed := IF(@has_ai_elf_schedule_completed = 0, 'ALTER TABLE `ai_elf_schedule` ADD COLUMN `completed` TINYINT(1) NULL', 'SELECT 1');
PREPARE s_ai_elf_schedule_completed FROM @ddl_ai_elf_schedule_completed;
EXECUTE s_ai_elf_schedule_completed;
DEALLOCATE PREPARE s_ai_elf_schedule_completed;

SET @has_ai_elf_schedule_updated_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_schedule' AND COLUMN_NAME = 'updated_at');
SET @ddl_ai_elf_schedule_updated_at := IF(@has_ai_elf_schedule_updated_at = 0, 'ALTER TABLE `ai_elf_schedule` ADD COLUMN `updated_at` DATETIME NULL', 'SELECT 1');
PREPARE s_ai_elf_schedule_updated_at FROM @ddl_ai_elf_schedule_updated_at;
EXECUTE s_ai_elf_schedule_updated_at;
DEALLOCATE PREPARE s_ai_elf_schedule_updated_at;

SET @has_ai_elf_sleep_notes := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_sleep' AND COLUMN_NAME = 'notes');
SET @ddl_ai_elf_sleep_notes := IF(@has_ai_elf_sleep_notes = 0, 'ALTER TABLE `ai_elf_sleep` ADD COLUMN `notes` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_ai_elf_sleep_notes FROM @ddl_ai_elf_sleep_notes;
EXECUTE s_ai_elf_sleep_notes;
DEALLOCATE PREPARE s_ai_elf_sleep_notes;

SET @has_ai_elf_sleep_record_date := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_elf_sleep' AND COLUMN_NAME = 'record_date');
SET @ddl_ai_elf_sleep_record_date := IF(@has_ai_elf_sleep_record_date = 0, 'ALTER TABLE `ai_elf_sleep` ADD COLUMN `record_date` DATE NULL', 'SELECT 1');
PREPARE s_ai_elf_sleep_record_date FROM @ddl_ai_elf_sleep_record_date;
EXECUTE s_ai_elf_sleep_record_date;
DEALLOCATE PREPARE s_ai_elf_sleep_record_date;

SET @has_character_preference_preferences := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'character_preference' AND COLUMN_NAME = 'preferences');
SET @ddl_character_preference_preferences := IF(@has_character_preference_preferences = 0, 'ALTER TABLE `character_preference` ADD COLUMN `preferences` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_character_preference_preferences FROM @ddl_character_preference_preferences;
EXECUTE s_character_preference_preferences;
DEALLOCATE PREPARE s_character_preference_preferences;

SET @has_character_preference_last_used_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'character_preference' AND COLUMN_NAME = 'last_used_at');
SET @ddl_character_preference_last_used_at := IF(@has_character_preference_last_used_at = 0, 'ALTER TABLE `character_preference` ADD COLUMN `last_used_at` DATETIME NULL', 'SELECT 1');
PREPARE s_character_preference_last_used_at FROM @ddl_character_preference_last_used_at;
EXECUTE s_character_preference_last_used_at;
DEALLOCATE PREPARE s_character_preference_last_used_at;

SET @has_community_post_media_urls := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_post' AND COLUMN_NAME = 'media_urls');
SET @ddl_community_post_media_urls := IF(@has_community_post_media_urls = 0, 'ALTER TABLE `community_post` ADD COLUMN `media_urls` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_community_post_media_urls FROM @ddl_community_post_media_urls;
EXECUTE s_community_post_media_urls;
DEALLOCATE PREPARE s_community_post_media_urls;

SET @has_conversation_summary_session_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'conversation_summary' AND COLUMN_NAME = 'session_id');
SET @ddl_conversation_summary_session_id := IF(@has_conversation_summary_session_id = 0, 'ALTER TABLE `conversation_summary` ADD COLUMN `session_id` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_conversation_summary_session_id FROM @ddl_conversation_summary_session_id;
EXECUTE s_conversation_summary_session_id;
DEALLOCATE PREPARE s_conversation_summary_session_id;

SET @has_conversation_summary_character_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'conversation_summary' AND COLUMN_NAME = 'character_id');
SET @ddl_conversation_summary_character_id := IF(@has_conversation_summary_character_id = 0, 'ALTER TABLE `conversation_summary` ADD COLUMN `character_id` BIGINT NULL', 'SELECT 1');
PREPARE s_conversation_summary_character_id FROM @ddl_conversation_summary_character_id;
EXECUTE s_conversation_summary_character_id;
DEALLOCATE PREPARE s_conversation_summary_character_id;

SET @has_conversation_summary_topics := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'conversation_summary' AND COLUMN_NAME = 'topics');
SET @ddl_conversation_summary_topics := IF(@has_conversation_summary_topics = 0, 'ALTER TABLE `conversation_summary` ADD COLUMN `topics` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_conversation_summary_topics FROM @ddl_conversation_summary_topics;
EXECUTE s_conversation_summary_topics;
DEALLOCATE PREPARE s_conversation_summary_topics;

SET @has_conversation_summary_message_count := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'conversation_summary' AND COLUMN_NAME = 'message_count');
SET @ddl_conversation_summary_message_count := IF(@has_conversation_summary_message_count = 0, 'ALTER TABLE `conversation_summary` ADD COLUMN `message_count` INT NULL', 'SELECT 1');
PREPARE s_conversation_summary_message_count FROM @ddl_conversation_summary_message_count;
EXECUTE s_conversation_summary_message_count;
DEALLOCATE PREPARE s_conversation_summary_message_count;

SET @has_elf_voice_command_executed := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'elf_voice_command' AND COLUMN_NAME = 'executed');
SET @ddl_elf_voice_command_executed := IF(@has_elf_voice_command_executed = 0, 'ALTER TABLE `elf_voice_command` ADD COLUMN `executed` TINYINT(1) NULL', 'SELECT 1');
PREPARE s_elf_voice_command_executed FROM @ddl_elf_voice_command_executed;
EXECUTE s_elf_voice_command_executed;
DEALLOCATE PREPARE s_elf_voice_command_executed;

SET @has_friend_group_member_remark := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'friend_group_member' AND COLUMN_NAME = 'remark');
SET @ddl_friend_group_member_remark := IF(@has_friend_group_member_remark = 0, 'ALTER TABLE `friend_group_member` ADD COLUMN `remark` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_friend_group_member_remark FROM @ddl_friend_group_member_remark;
EXECUTE s_friend_group_member_remark;
DEALLOCATE PREPARE s_friend_group_member_remark;

SET @has_friend_group_member_created_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'friend_group_member' AND COLUMN_NAME = 'created_at');
SET @ddl_friend_group_member_created_at := IF(@has_friend_group_member_created_at = 0, 'ALTER TABLE `friend_group_member` ADD COLUMN `created_at` DATETIME NULL', 'SELECT 1');
PREPARE s_friend_group_member_created_at FROM @ddl_friend_group_member_created_at;
EXECUTE s_friend_group_member_created_at;
DEALLOCATE PREPARE s_friend_group_member_created_at;

SET @has_friendship_username := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'friendship' AND COLUMN_NAME = 'username');
SET @ddl_friendship_username := IF(@has_friendship_username = 0, 'ALTER TABLE `friendship` ADD COLUMN `username` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_friendship_username FROM @ddl_friendship_username;
EXECUTE s_friendship_username;
DEALLOCATE PREPARE s_friendship_username;

SET @has_friendship_nickname := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'friendship' AND COLUMN_NAME = 'nickname');
SET @ddl_friendship_nickname := IF(@has_friendship_nickname = 0, 'ALTER TABLE `friendship` ADD COLUMN `nickname` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_friendship_nickname FROM @ddl_friendship_nickname;
EXECUTE s_friendship_nickname;
DEALLOCATE PREPARE s_friendship_nickname;

SET @has_friendship_avatar := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'friendship' AND COLUMN_NAME = 'avatar');
SET @ddl_friendship_avatar := IF(@has_friendship_avatar = 0, 'ALTER TABLE `friendship` ADD COLUMN `avatar` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_friendship_avatar FROM @ddl_friendship_avatar;
EXECUTE s_friendship_avatar;
DEALLOCATE PREPARE s_friendship_avatar;

SET @has_group_chat_owner_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'group_chat' AND COLUMN_NAME = 'owner_id');
SET @ddl_group_chat_owner_id := IF(@has_group_chat_owner_id = 0, 'ALTER TABLE `group_chat` ADD COLUMN `owner_id` BIGINT NULL', 'SELECT 1');
PREPARE s_group_chat_owner_id FROM @ddl_group_chat_owner_id;
EXECUTE s_group_chat_owner_id;
DEALLOCATE PREPARE s_group_chat_owner_id;

SET @has_group_chat_member_count := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'group_chat' AND COLUMN_NAME = 'member_count');
SET @ddl_group_chat_member_count := IF(@has_group_chat_member_count = 0, 'ALTER TABLE `group_chat` ADD COLUMN `member_count` INT NULL', 'SELECT 1');
PREPARE s_group_chat_member_count FROM @ddl_group_chat_member_count;
EXECUTE s_group_chat_member_count;
DEALLOCATE PREPARE s_group_chat_member_count;

SET @has_group_chat_is_public := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'group_chat' AND COLUMN_NAME = 'is_public');
SET @ddl_group_chat_is_public := IF(@has_group_chat_is_public = 0, 'ALTER TABLE `group_chat` ADD COLUMN `is_public` TINYINT(1) NULL', 'SELECT 1');
PREPARE s_group_chat_is_public FROM @ddl_group_chat_is_public;
EXECUTE s_group_chat_is_public;
DEALLOCATE PREPARE s_group_chat_is_public;

SET @has_group_chat_created_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'group_chat' AND COLUMN_NAME = 'created_at');
SET @ddl_group_chat_created_at := IF(@has_group_chat_created_at = 0, 'ALTER TABLE `group_chat` ADD COLUMN `created_at` DATETIME NULL', 'SELECT 1');
PREPARE s_group_chat_created_at FROM @ddl_group_chat_created_at;
EXECUTE s_group_chat_created_at;
DEALLOCATE PREPARE s_group_chat_created_at;

SET @has_group_member_joined_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'group_member' AND COLUMN_NAME = 'joined_at');
SET @ddl_group_member_joined_at := IF(@has_group_member_joined_at = 0, 'ALTER TABLE `group_member` ADD COLUMN `joined_at` DATETIME NULL', 'SELECT 1');
PREPARE s_group_member_joined_at FROM @ddl_group_member_joined_at;
EXECUTE s_group_member_joined_at;
DEALLOCATE PREPARE s_group_member_joined_at;

SET @has_group_message_user_name := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'group_message' AND COLUMN_NAME = 'user_name');
SET @ddl_group_message_user_name := IF(@has_group_message_user_name = 0, 'ALTER TABLE `group_message` ADD COLUMN `user_name` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_group_message_user_name FROM @ddl_group_message_user_name;
EXECUTE s_group_message_user_name;
DEALLOCATE PREPARE s_group_message_user_name;

SET @has_group_message_media_type := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'group_message' AND COLUMN_NAME = 'media_type');
SET @ddl_group_message_media_type := IF(@has_group_message_media_type = 0, 'ALTER TABLE `group_message` ADD COLUMN `media_type` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_group_message_media_type FROM @ddl_group_message_media_type;
EXECUTE s_group_message_media_type;
DEALLOCATE PREPARE s_group_message_media_type;

SET @has_group_message_media_url := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'group_message' AND COLUMN_NAME = 'media_url');
SET @ddl_group_message_media_url := IF(@has_group_message_media_url = 0, 'ALTER TABLE `group_message` ADD COLUMN `media_url` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_group_message_media_url FROM @ddl_group_message_media_url;
EXECUTE s_group_message_media_url;
DEALLOCATE PREPARE s_group_message_media_url;

SET @has_group_message_is_read := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'group_message' AND COLUMN_NAME = 'is_read');
SET @ddl_group_message_is_read := IF(@has_group_message_is_read = 0, 'ALTER TABLE `group_message` ADD COLUMN `is_read` TINYINT(1) NULL', 'SELECT 1');
PREPARE s_group_message_is_read FROM @ddl_group_message_is_read;
EXECUTE s_group_message_is_read;
DEALLOCATE PREPARE s_group_message_is_read;

SET @has_health_analysis_record_analysis_date := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'health_analysis_record' AND COLUMN_NAME = 'analysis_date');
SET @ddl_health_analysis_record_analysis_date := IF(@has_health_analysis_record_analysis_date = 0, 'ALTER TABLE `health_analysis_record` ADD COLUMN `analysis_date` DATE NULL', 'SELECT 1');
PREPARE s_health_analysis_record_analysis_date FROM @ddl_health_analysis_record_analysis_date;
EXECUTE s_health_analysis_record_analysis_date;
DEALLOCATE PREPARE s_health_analysis_record_analysis_date;

SET @has_health_analysis_record_focus_duration := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'health_analysis_record' AND COLUMN_NAME = 'focus_duration');
SET @ddl_health_analysis_record_focus_duration := IF(@has_health_analysis_record_focus_duration = 0, 'ALTER TABLE `health_analysis_record` ADD COLUMN `focus_duration` INT NULL', 'SELECT 1');
PREPARE s_health_analysis_record_focus_duration FROM @ddl_health_analysis_record_focus_duration;
EXECUTE s_health_analysis_record_focus_duration;
DEALLOCATE PREPARE s_health_analysis_record_focus_duration;

SET @has_health_analysis_record_sleep_quality := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'health_analysis_record' AND COLUMN_NAME = 'sleep_quality');
SET @ddl_health_analysis_record_sleep_quality := IF(@has_health_analysis_record_sleep_quality = 0, 'ALTER TABLE `health_analysis_record` ADD COLUMN `sleep_quality` INT NULL', 'SELECT 1');
PREPARE s_health_analysis_record_sleep_quality FROM @ddl_health_analysis_record_sleep_quality;
EXECUTE s_health_analysis_record_sleep_quality;
DEALLOCATE PREPARE s_health_analysis_record_sleep_quality;

SET @has_health_analysis_record_step_count := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'health_analysis_record' AND COLUMN_NAME = 'step_count');
SET @ddl_health_analysis_record_step_count := IF(@has_health_analysis_record_step_count = 0, 'ALTER TABLE `health_analysis_record` ADD COLUMN `step_count` INT NULL', 'SELECT 1');
PREPARE s_health_analysis_record_step_count FROM @ddl_health_analysis_record_step_count;
EXECUTE s_health_analysis_record_step_count;
DEALLOCATE PREPARE s_health_analysis_record_step_count;

SET @has_health_analysis_record_mood_score := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'health_analysis_record' AND COLUMN_NAME = 'mood_score');
SET @ddl_health_analysis_record_mood_score := IF(@has_health_analysis_record_mood_score = 0, 'ALTER TABLE `health_analysis_record` ADD COLUMN `mood_score` INT NULL', 'SELECT 1');
PREPARE s_health_analysis_record_mood_score FROM @ddl_health_analysis_record_mood_score;
EXECUTE s_health_analysis_record_mood_score;
DEALLOCATE PREPARE s_health_analysis_record_mood_score;

SET @has_health_analysis_record_health_score := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'health_analysis_record' AND COLUMN_NAME = 'health_score');
SET @ddl_health_analysis_record_health_score := IF(@has_health_analysis_record_health_score = 0, 'ALTER TABLE `health_analysis_record` ADD COLUMN `health_score` INT NULL', 'SELECT 1');
PREPARE s_health_analysis_record_health_score FROM @ddl_health_analysis_record_health_score;
EXECUTE s_health_analysis_record_health_score;
DEALLOCATE PREPARE s_health_analysis_record_health_score;

SET @has_health_analysis_record_suggestions := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'health_analysis_record' AND COLUMN_NAME = 'suggestions');
SET @ddl_health_analysis_record_suggestions := IF(@has_health_analysis_record_suggestions = 0, 'ALTER TABLE `health_analysis_record` ADD COLUMN `suggestions` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_health_analysis_record_suggestions FROM @ddl_health_analysis_record_suggestions;
EXECUTE s_health_analysis_record_suggestions;
DEALLOCATE PREPARE s_health_analysis_record_suggestions;

SET @has_login_alert_device_name := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_alert' AND COLUMN_NAME = 'device_name');
SET @ddl_login_alert_device_name := IF(@has_login_alert_device_name = 0, 'ALTER TABLE `login_alert` ADD COLUMN `device_name` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_login_alert_device_name FROM @ddl_login_alert_device_name;
EXECUTE s_login_alert_device_name;
DEALLOCATE PREPARE s_login_alert_device_name;

SET @has_login_alert_login_time := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_alert' AND COLUMN_NAME = 'login_time');
SET @ddl_login_alert_login_time := IF(@has_login_alert_login_time = 0, 'ALTER TABLE `login_alert` ADD COLUMN `login_time` DATETIME NULL', 'SELECT 1');
PREPARE s_login_alert_login_time FROM @ddl_login_alert_login_time;
EXECUTE s_login_alert_login_time;
DEALLOCATE PREPARE s_login_alert_login_time;

SET @has_login_alert_status := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_alert' AND COLUMN_NAME = 'status');
SET @ddl_login_alert_status := IF(@has_login_alert_status = 0, 'ALTER TABLE `login_alert` ADD COLUMN `status` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_login_alert_status FROM @ddl_login_alert_status;
EXECUTE s_login_alert_status;
DEALLOCATE PREPARE s_login_alert_status;

SET @has_login_device_os := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_device' AND COLUMN_NAME = 'os');
SET @ddl_login_device_os := IF(@has_login_device_os = 0, 'ALTER TABLE `login_device` ADD COLUMN `os` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_login_device_os FROM @ddl_login_device_os;
EXECUTE s_login_device_os;
DEALLOCATE PREPARE s_login_device_os;

SET @has_login_device_browser := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_device' AND COLUMN_NAME = 'browser');
SET @ddl_login_device_browser := IF(@has_login_device_browser = 0, 'ALTER TABLE `login_device` ADD COLUMN `browser` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_login_device_browser FROM @ddl_login_device_browser;
EXECUTE s_login_device_browser;
DEALLOCATE PREPARE s_login_device_browser;

SET @has_pay_account_account_number := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_account' AND COLUMN_NAME = 'account_number');
SET @ddl_pay_account_account_number := IF(@has_pay_account_account_number = 0, 'ALTER TABLE `pay_account` ADD COLUMN `account_number` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_pay_account_account_number FROM @ddl_pay_account_account_number;
EXECUTE s_pay_account_account_number;
DEALLOCATE PREPARE s_pay_account_account_number;

SET @has_pay_account_account_name := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_account' AND COLUMN_NAME = 'account_name');
SET @ddl_pay_account_account_name := IF(@has_pay_account_account_name = 0, 'ALTER TABLE `pay_account` ADD COLUMN `account_name` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_pay_account_account_name FROM @ddl_pay_account_account_name;
EXECUTE s_pay_account_account_name;
DEALLOCATE PREPARE s_pay_account_account_name;

SET @has_pay_account_verification_code := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_account' AND COLUMN_NAME = 'verification_code');
SET @ddl_pay_account_verification_code := IF(@has_pay_account_verification_code = 0, 'ALTER TABLE `pay_account` ADD COLUMN `verification_code` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_pay_account_verification_code FROM @ddl_pay_account_verification_code;
EXECUTE s_pay_account_verification_code;
DEALLOCATE PREPARE s_pay_account_verification_code;

SET @has_pay_account_verified_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_account' AND COLUMN_NAME = 'verified_at');
SET @ddl_pay_account_verified_at := IF(@has_pay_account_verified_at = 0, 'ALTER TABLE `pay_account` ADD COLUMN `verified_at` DATETIME NULL', 'SELECT 1');
PREPARE s_pay_account_verified_at FROM @ddl_pay_account_verified_at;
EXECUTE s_pay_account_verified_at;
DEALLOCATE PREPARE s_pay_account_verified_at;

SET @has_pay_account_create_time := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_account' AND COLUMN_NAME = 'create_time');
SET @ddl_pay_account_create_time := IF(@has_pay_account_create_time = 0, 'ALTER TABLE `pay_account` ADD COLUMN `create_time` DATETIME NULL', 'SELECT 1');
PREPARE s_pay_account_create_time FROM @ddl_pay_account_create_time;
EXECUTE s_pay_account_create_time;
DEALLOCATE PREPARE s_pay_account_create_time;

SET @has_pay_account_update_time := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_account' AND COLUMN_NAME = 'update_time');
SET @ddl_pay_account_update_time := IF(@has_pay_account_update_time = 0, 'ALTER TABLE `pay_account` ADD COLUMN `update_time` DATETIME NULL', 'SELECT 1');
PREPARE s_pay_account_update_time FROM @ddl_pay_account_update_time;
EXECUTE s_pay_account_update_time;
DEALLOCATE PREPARE s_pay_account_update_time;

SET @has_privacy_setting_key := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'privacy_setting' AND COLUMN_NAME = 'key');
SET @ddl_privacy_setting_key := IF(@has_privacy_setting_key = 0, 'ALTER TABLE `privacy_setting` ADD COLUMN `key` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_privacy_setting_key FROM @ddl_privacy_setting_key;
EXECUTE s_privacy_setting_key;
DEALLOCATE PREPARE s_privacy_setting_key;

SET @has_privacy_setting_value := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'privacy_setting' AND COLUMN_NAME = 'value');
SET @ddl_privacy_setting_value := IF(@has_privacy_setting_value = 0, 'ALTER TABLE `privacy_setting` ADD COLUMN `value` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_privacy_setting_value FROM @ddl_privacy_setting_value;
EXECUTE s_privacy_setting_value;
DEALLOCATE PREPARE s_privacy_setting_value;

SET @has_sleep_analysis_report_avg_duration := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sleep_analysis_report' AND COLUMN_NAME = 'avg_duration');
SET @ddl_sleep_analysis_report_avg_duration := IF(@has_sleep_analysis_report_avg_duration = 0, 'ALTER TABLE `sleep_analysis_report` ADD COLUMN `avg_duration` DOUBLE NULL', 'SELECT 1');
PREPARE s_sleep_analysis_report_avg_duration FROM @ddl_sleep_analysis_report_avg_duration;
EXECUTE s_sleep_analysis_report_avg_duration;
DEALLOCATE PREPARE s_sleep_analysis_report_avg_duration;

SET @has_sleep_analysis_report_avg_quality := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sleep_analysis_report' AND COLUMN_NAME = 'avg_quality');
SET @ddl_sleep_analysis_report_avg_quality := IF(@has_sleep_analysis_report_avg_quality = 0, 'ALTER TABLE `sleep_analysis_report` ADD COLUMN `avg_quality` INT NULL', 'SELECT 1');
PREPARE s_sleep_analysis_report_avg_quality FROM @ddl_sleep_analysis_report_avg_quality;
EXECUTE s_sleep_analysis_report_avg_quality;
DEALLOCATE PREPARE s_sleep_analysis_report_avg_quality;

SET @has_sleep_analysis_report_deep_sleep_ratio := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sleep_analysis_report' AND COLUMN_NAME = 'deep_sleep_ratio');
SET @ddl_sleep_analysis_report_deep_sleep_ratio := IF(@has_sleep_analysis_report_deep_sleep_ratio = 0, 'ALTER TABLE `sleep_analysis_report` ADD COLUMN `deep_sleep_ratio` DOUBLE NULL', 'SELECT 1');
PREPARE s_sleep_analysis_report_deep_sleep_ratio FROM @ddl_sleep_analysis_report_deep_sleep_ratio;
EXECUTE s_sleep_analysis_report_deep_sleep_ratio;
DEALLOCATE PREPARE s_sleep_analysis_report_deep_sleep_ratio;

SET @has_sms_code_expire_time := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sms_code' AND COLUMN_NAME = 'expire_time');
SET @ddl_sms_code_expire_time := IF(@has_sms_code_expire_time = 0, 'ALTER TABLE `sms_code` ADD COLUMN `expire_time` DATETIME NULL', 'SELECT 1');
PREPARE s_sms_code_expire_time FROM @ddl_sms_code_expire_time;
EXECUTE s_sms_code_expire_time;
DEALLOCATE PREPARE s_sms_code_expire_time;

SET @has_sms_code_used := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sms_code' AND COLUMN_NAME = 'used');
SET @ddl_sms_code_used := IF(@has_sms_code_used = 0, 'ALTER TABLE `sms_code` ADD COLUMN `used` TINYINT(1) NULL', 'SELECT 1');
PREPARE s_sms_code_used FROM @ddl_sms_code_used;
EXECUTE s_sms_code_used;
DEALLOCATE PREPARE s_sms_code_used;

SET @has_social_post_tag_created_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'social_post_tag' AND COLUMN_NAME = 'created_at');
SET @ddl_social_post_tag_created_at := IF(@has_social_post_tag_created_at = 0, 'ALTER TABLE `social_post_tag` ADD COLUMN `created_at` DATETIME NULL', 'SELECT 1');
PREPARE s_social_post_tag_created_at FROM @ddl_social_post_tag_created_at;
EXECUTE s_social_post_tag_created_at;
DEALLOCATE PREPARE s_social_post_tag_created_at;

SET @has_social_share_user_name := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'social_share' AND COLUMN_NAME = 'user_name');
SET @ddl_social_share_user_name := IF(@has_social_share_user_name = 0, 'ALTER TABLE `social_share` ADD COLUMN `user_name` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_social_share_user_name FROM @ddl_social_share_user_name;
EXECUTE s_social_share_user_name;
DEALLOCATE PREPARE s_social_share_user_name;

SET @has_social_share_share_text := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'social_share' AND COLUMN_NAME = 'share_text');
SET @ddl_social_share_share_text := IF(@has_social_share_share_text = 0, 'ALTER TABLE `social_share` ADD COLUMN `share_text` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_social_share_share_text FROM @ddl_social_share_share_text;
EXECUTE s_social_share_share_text;
DEALLOCATE PREPARE s_social_share_share_text;

SET @has_social_share_target_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'social_share' AND COLUMN_NAME = 'target_id');
SET @ddl_social_share_target_id := IF(@has_social_share_target_id = 0, 'ALTER TABLE `social_share` ADD COLUMN `target_id` BIGINT NULL', 'SELECT 1');
PREPARE s_social_share_target_id FROM @ddl_social_share_target_id;
EXECUTE s_social_share_target_id;
DEALLOCATE PREPARE s_social_share_target_id;

SET @has_user_badge_badge_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_badge' AND COLUMN_NAME = 'badge_id');
SET @ddl_user_badge_badge_id := IF(@has_user_badge_badge_id = 0, 'ALTER TABLE `user_badge` ADD COLUMN `badge_id` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_user_badge_badge_id FROM @ddl_user_badge_badge_id;
EXECUTE s_user_badge_badge_id;
DEALLOCATE PREPARE s_user_badge_badge_id;

SET @has_user_badge_badge_icon := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_badge' AND COLUMN_NAME = 'badge_icon');
SET @ddl_user_badge_badge_icon := IF(@has_user_badge_badge_icon = 0, 'ALTER TABLE `user_badge` ADD COLUMN `badge_icon` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_user_badge_badge_icon FROM @ddl_user_badge_badge_icon;
EXECUTE s_user_badge_badge_icon;
DEALLOCATE PREPARE s_user_badge_badge_icon;

SET @has_user_badge_description := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_badge' AND COLUMN_NAME = 'description');
SET @ddl_user_badge_description := IF(@has_user_badge_description = 0, 'ALTER TABLE `user_badge` ADD COLUMN `description` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_user_badge_description FROM @ddl_user_badge_description;
EXECUTE s_user_badge_description;
DEALLOCATE PREPARE s_user_badge_description;

SET @has_user_block_blocked_user_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_block' AND COLUMN_NAME = 'blocked_user_id');
SET @ddl_user_block_blocked_user_id := IF(@has_user_block_blocked_user_id = 0, 'ALTER TABLE `user_block` ADD COLUMN `blocked_user_id` BIGINT NULL', 'SELECT 1');
PREPARE s_user_block_blocked_user_id FROM @ddl_user_block_blocked_user_id;
EXECUTE s_user_block_blocked_user_id;
DEALLOCATE PREPARE s_user_block_blocked_user_id;

SET @has_user_block_blocked_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_block' AND COLUMN_NAME = 'blocked_at');
SET @ddl_user_block_blocked_at := IF(@has_user_block_blocked_at = 0, 'ALTER TABLE `user_block` ADD COLUMN `blocked_at` DATETIME NULL', 'SELECT 1');
PREPARE s_user_block_blocked_at FROM @ddl_user_block_blocked_at;
EXECUTE s_user_block_blocked_at;
DEALLOCATE PREPARE s_user_block_blocked_at;

SET @has_user_interest_interest := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_interest' AND COLUMN_NAME = 'interest');
SET @ddl_user_interest_interest := IF(@has_user_interest_interest = 0, 'ALTER TABLE `user_interest` ADD COLUMN `interest` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_user_interest_interest FROM @ddl_user_interest_interest;
EXECUTE s_user_interest_interest;
DEALLOCATE PREPARE s_user_interest_interest;

SET @has_user_interest_weight := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_interest' AND COLUMN_NAME = 'weight');
SET @ddl_user_interest_weight := IF(@has_user_interest_weight = 0, 'ALTER TABLE `user_interest` ADD COLUMN `weight` INT NULL', 'SELECT 1');
PREPARE s_user_interest_weight FROM @ddl_user_interest_weight;
EXECUTE s_user_interest_weight;
DEALLOCATE PREPARE s_user_interest_weight;

SET @has_user_interest_updated_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_interest' AND COLUMN_NAME = 'updated_at');
SET @ddl_user_interest_updated_at := IF(@has_user_interest_updated_at = 0, 'ALTER TABLE `user_interest` ADD COLUMN `updated_at` DATETIME NULL', 'SELECT 1');
PREPARE s_user_interest_updated_at FROM @ddl_user_interest_updated_at;
EXECUTE s_user_interest_updated_at;
DEALLOCATE PREPARE s_user_interest_updated_at;

SET @has_user_report_user_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_report' AND COLUMN_NAME = 'user_id');
SET @ddl_user_report_user_id := IF(@has_user_report_user_id = 0, 'ALTER TABLE `user_report` ADD COLUMN `user_id` BIGINT NULL', 'SELECT 1');
PREPARE s_user_report_user_id FROM @ddl_user_report_user_id;
EXECUTE s_user_report_user_id;
DEALLOCATE PREPARE s_user_report_user_id;

SET @has_user_report_target_type := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_report' AND COLUMN_NAME = 'target_type');
SET @ddl_user_report_target_type := IF(@has_user_report_target_type = 0, 'ALTER TABLE `user_report` ADD COLUMN `target_type` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_user_report_target_type FROM @ddl_user_report_target_type;
EXECUTE s_user_report_target_type;
DEALLOCATE PREPARE s_user_report_target_type;

SET @has_user_report_target_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_report' AND COLUMN_NAME = 'target_id');
SET @ddl_user_report_target_id := IF(@has_user_report_target_id = 0, 'ALTER TABLE `user_report` ADD COLUMN `target_id` BIGINT NULL', 'SELECT 1');
PREPARE s_user_report_target_id FROM @ddl_user_report_target_id;
EXECUTE s_user_report_target_id;
DEALLOCATE PREPARE s_user_report_target_id;

SET @has_user_report_report_reason := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_report' AND COLUMN_NAME = 'report_reason');
SET @ddl_user_report_report_reason := IF(@has_user_report_report_reason = 0, 'ALTER TABLE `user_report` ADD COLUMN `report_reason` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_user_report_report_reason FROM @ddl_user_report_report_reason;
EXECUTE s_user_report_report_reason;
DEALLOCATE PREPARE s_user_report_report_reason;

SET @has_user_report_review_result := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_report' AND COLUMN_NAME = 'review_result');
SET @ddl_user_report_review_result := IF(@has_user_report_review_result = 0, 'ALTER TABLE `user_report` ADD COLUMN `review_result` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_user_report_review_result FROM @ddl_user_report_review_result;
EXECUTE s_user_report_review_result;
DEALLOCATE PREPARE s_user_report_review_result;

SET @has_user_report_reviewed_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_report' AND COLUMN_NAME = 'reviewed_at');
SET @ddl_user_report_reviewed_at := IF(@has_user_report_reviewed_at = 0, 'ALTER TABLE `user_report` ADD COLUMN `reviewed_at` DATETIME NULL', 'SELECT 1');
PREPARE s_user_report_reviewed_at FROM @ddl_user_report_reviewed_at;
EXECUTE s_user_report_reviewed_at;
DEALLOCATE PREPARE s_user_report_reviewed_at;

SET @has_user_statistics_post_count := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_statistics' AND COLUMN_NAME = 'post_count');
SET @ddl_user_statistics_post_count := IF(@has_user_statistics_post_count = 0, 'ALTER TABLE `user_statistics` ADD COLUMN `post_count` INT NULL', 'SELECT 1');
PREPARE s_user_statistics_post_count FROM @ddl_user_statistics_post_count;
EXECUTE s_user_statistics_post_count;
DEALLOCATE PREPARE s_user_statistics_post_count;

SET @has_user_statistics_comment_count := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_statistics' AND COLUMN_NAME = 'comment_count');
SET @ddl_user_statistics_comment_count := IF(@has_user_statistics_comment_count = 0, 'ALTER TABLE `user_statistics` ADD COLUMN `comment_count` INT NULL', 'SELECT 1');
PREPARE s_user_statistics_comment_count FROM @ddl_user_statistics_comment_count;
EXECUTE s_user_statistics_comment_count;
DEALLOCATE PREPARE s_user_statistics_comment_count;

SET @has_user_statistics_like_count := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_statistics' AND COLUMN_NAME = 'like_count');
SET @ddl_user_statistics_like_count := IF(@has_user_statistics_like_count = 0, 'ALTER TABLE `user_statistics` ADD COLUMN `like_count` INT NULL', 'SELECT 1');
PREPARE s_user_statistics_like_count FROM @ddl_user_statistics_like_count;
EXECUTE s_user_statistics_like_count;
DEALLOCATE PREPARE s_user_statistics_like_count;

SET @has_user_statistics_follower_count := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_statistics' AND COLUMN_NAME = 'follower_count');
SET @ddl_user_statistics_follower_count := IF(@has_user_statistics_follower_count = 0, 'ALTER TABLE `user_statistics` ADD COLUMN `follower_count` INT NULL', 'SELECT 1');
PREPARE s_user_statistics_follower_count FROM @ddl_user_statistics_follower_count;
EXECUTE s_user_statistics_follower_count;
DEALLOCATE PREPARE s_user_statistics_follower_count;

SET @has_user_statistics_following_count := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_statistics' AND COLUMN_NAME = 'following_count');
SET @ddl_user_statistics_following_count := IF(@has_user_statistics_following_count = 0, 'ALTER TABLE `user_statistics` ADD COLUMN `following_count` INT NULL', 'SELECT 1');
PREPARE s_user_statistics_following_count FROM @ddl_user_statistics_following_count;
EXECUTE s_user_statistics_following_count;
DEALLOCATE PREPARE s_user_statistics_following_count;

SET @has_user_statistics_share_count := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_statistics' AND COLUMN_NAME = 'share_count');
SET @ddl_user_statistics_share_count := IF(@has_user_statistics_share_count = 0, 'ALTER TABLE `user_statistics` ADD COLUMN `share_count` INT NULL', 'SELECT 1');
PREPARE s_user_statistics_share_count FROM @ddl_user_statistics_share_count;
EXECUTE s_user_statistics_share_count;
DEALLOCATE PREPARE s_user_statistics_share_count;

SET @has_user_tag_tag_category := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_tag' AND COLUMN_NAME = 'tag_category');
SET @ddl_user_tag_tag_category := IF(@has_user_tag_tag_category = 0, 'ALTER TABLE `user_tag` ADD COLUMN `tag_category` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_user_tag_tag_category FROM @ddl_user_tag_tag_category;
EXECUTE s_user_tag_tag_category;
DEALLOCATE PREPARE s_user_tag_tag_category;

SET @has_user_view_record_target_type := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_view_record' AND COLUMN_NAME = 'target_type');
SET @ddl_user_view_record_target_type := IF(@has_user_view_record_target_type = 0, 'ALTER TABLE `user_view_record` ADD COLUMN `target_type` VARCHAR(255) NULL', 'SELECT 1');
PREPARE s_user_view_record_target_type FROM @ddl_user_view_record_target_type;
EXECUTE s_user_view_record_target_type;
DEALLOCATE PREPARE s_user_view_record_target_type;

SET @has_user_view_record_target_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_view_record' AND COLUMN_NAME = 'target_id');
SET @ddl_user_view_record_target_id := IF(@has_user_view_record_target_id = 0, 'ALTER TABLE `user_view_record` ADD COLUMN `target_id` BIGINT NULL', 'SELECT 1');
PREPARE s_user_view_record_target_id FROM @ddl_user_view_record_target_id;
EXECUTE s_user_view_record_target_id;
DEALLOCATE PREPARE s_user_view_record_target_id;
