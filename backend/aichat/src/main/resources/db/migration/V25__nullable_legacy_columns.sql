-- ============================================================
-- V25: 旧架构遗留 NOT NULL 列改为可空（实体不维护这些列）
-- 幂等：仅当列为 NOT NULL 时执行 MODIFY
-- ============================================================

SET @nn_conversation_summary_conversation_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='conversation_summary' AND COLUMN_NAME='conversation_id' AND IS_NULLABLE='NO');
SET @ddl_conversation_summary_conversation_id := IF(@nn_conversation_summary_conversation_id>0, 'ALTER TABLE `conversation_summary` MODIFY `conversation_id` varchar(100) NULL', 'SELECT 1');
PREPARE s_conversation_summary_conversation_id FROM @ddl_conversation_summary_conversation_id;
EXECUTE s_conversation_summary_conversation_id;
DEALLOCATE PREPARE s_conversation_summary_conversation_id;

SET @nn_group_chat_creator_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='group_chat' AND COLUMN_NAME='creator_id' AND IS_NULLABLE='NO');
SET @ddl_group_chat_creator_id := IF(@nn_group_chat_creator_id>0, 'ALTER TABLE `group_chat` MODIFY `creator_id` bigint NULL', 'SELECT 1');
PREPARE s_group_chat_creator_id FROM @ddl_group_chat_creator_id;
EXECUTE s_group_chat_creator_id;
DEALLOCATE PREPARE s_group_chat_creator_id;

SET @nn_privacy_setting_setting_key := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='privacy_setting' AND COLUMN_NAME='setting_key' AND IS_NULLABLE='NO');
SET @ddl_privacy_setting_setting_key := IF(@nn_privacy_setting_setting_key>0, 'ALTER TABLE `privacy_setting` MODIFY `setting_key` varchar(100) NULL', 'SELECT 1');
PREPARE s_privacy_setting_setting_key FROM @ddl_privacy_setting_setting_key;
EXECUTE s_privacy_setting_setting_key;
DEALLOCATE PREPARE s_privacy_setting_setting_key;

SET @nn_sms_code_expires_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sms_code' AND COLUMN_NAME='expires_at' AND IS_NULLABLE='NO');
SET @ddl_sms_code_expires_at := IF(@nn_sms_code_expires_at>0, 'ALTER TABLE `sms_code` MODIFY `expires_at` datetime NULL', 'SELECT 1');
PREPARE s_sms_code_expires_at FROM @ddl_sms_code_expires_at;
EXECUTE s_sms_code_expires_at;
DEALLOCATE PREPARE s_sms_code_expires_at;

SET @nn_user_block_block_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='user_block' AND COLUMN_NAME='block_id' AND IS_NULLABLE='NO');
SET @ddl_user_block_block_id := IF(@nn_user_block_block_id>0, 'ALTER TABLE `user_block` MODIFY `block_id` bigint NULL', 'SELECT 1');
PREPARE s_user_block_block_id FROM @ddl_user_block_block_id;
EXECUTE s_user_block_block_id;
DEALLOCATE PREPARE s_user_block_block_id;

SET @nn_user_interest_interest_name := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='user_interest' AND COLUMN_NAME='interest_name' AND IS_NULLABLE='NO');
SET @ddl_user_interest_interest_name := IF(@nn_user_interest_interest_name>0, 'ALTER TABLE `user_interest` MODIFY `interest_name` varchar(50) NULL', 'SELECT 1');
PREPARE s_user_interest_interest_name FROM @ddl_user_interest_interest_name;
EXECUTE s_user_interest_interest_name;
DEALLOCATE PREPARE s_user_interest_interest_name;

SET @nn_user_report_reporter_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='user_report' AND COLUMN_NAME='reporter_id' AND IS_NULLABLE='NO');
SET @ddl_user_report_reporter_id := IF(@nn_user_report_reporter_id>0, 'ALTER TABLE `user_report` MODIFY `reporter_id` bigint NULL', 'SELECT 1');
PREPARE s_user_report_reporter_id FROM @ddl_user_report_reporter_id;
EXECUTE s_user_report_reporter_id;
DEALLOCATE PREPARE s_user_report_reporter_id;

SET @nn_user_report_reported_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='user_report' AND COLUMN_NAME='reported_id' AND IS_NULLABLE='NO');
SET @ddl_user_report_reported_id := IF(@nn_user_report_reported_id>0, 'ALTER TABLE `user_report` MODIFY `reported_id` bigint NULL', 'SELECT 1');
PREPARE s_user_report_reported_id FROM @ddl_user_report_reported_id;
EXECUTE s_user_report_reported_id;
DEALLOCATE PREPARE s_user_report_reported_id;
