-- ============================================================
-- V31: feed_task 表补充多文件/图片/知识库关联列
--   file_paths / file_names     : 前端一次提交多个文件时的路径与原始名(逗号分隔)
--   image_paths / image_names   : 前端一次提交多张图片时的路径与原始名(逗号分隔)
--   document_id                 : 投喂文本/文本文件入库知识库后关联的 knowledge_document.id
--   knowledge_points            : 本次投喂提取的知识点(分块)数量
-- 幂等：仅当列不存在时执行 ADD COLUMN（MySQL 8 不支持 IF NOT EXISTS 加列）
-- ============================================================

SET @col_feed_file_paths := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feed_task' AND COLUMN_NAME='file_paths');
SET @ddl_feed_file_paths := IF(@col_feed_file_paths=0, "ALTER TABLE `feed_task` ADD COLUMN `file_paths` VARCHAR(2000) DEFAULT NULL COMMENT '多文件路径(逗号分隔)'", 'SELECT 1');
PREPARE s_feed_file_paths FROM @ddl_feed_file_paths;
EXECUTE s_feed_file_paths;
DEALLOCATE PREPARE s_feed_file_paths;

SET @col_feed_file_names := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feed_task' AND COLUMN_NAME='file_names');
SET @ddl_feed_file_names := IF(@col_feed_file_names=0, "ALTER TABLE `feed_task` ADD COLUMN `file_names` VARCHAR(2000) DEFAULT NULL COMMENT '多原始文件名(逗号分隔)'", 'SELECT 1');
PREPARE s_feed_file_names FROM @ddl_feed_file_names;
EXECUTE s_feed_file_names;
DEALLOCATE PREPARE s_feed_file_names;

SET @col_feed_image_paths := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feed_task' AND COLUMN_NAME='image_paths');
SET @ddl_feed_image_paths := IF(@col_feed_image_paths=0, "ALTER TABLE `feed_task` ADD COLUMN `image_paths` VARCHAR(2000) DEFAULT NULL COMMENT '多图片路径(逗号分隔)'", 'SELECT 1');
PREPARE s_feed_image_paths FROM @ddl_feed_image_paths;
EXECUTE s_feed_image_paths;
DEALLOCATE PREPARE s_feed_image_paths;

SET @col_feed_image_names := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feed_task' AND COLUMN_NAME='image_names');
SET @ddl_feed_image_names := IF(@col_feed_image_names=0, "ALTER TABLE `feed_task` ADD COLUMN `image_names` VARCHAR(2000) DEFAULT NULL COMMENT '多图片原始名(逗号分隔)'", 'SELECT 1');
PREPARE s_feed_image_names FROM @ddl_feed_image_names;
EXECUTE s_feed_image_names;
DEALLOCATE PREPARE s_feed_image_names;

SET @col_feed_document_id := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feed_task' AND COLUMN_NAME='document_id');
SET @ddl_feed_document_id := IF(@col_feed_document_id=0, "ALTER TABLE `feed_task` ADD COLUMN `document_id` BIGINT DEFAULT NULL COMMENT '关联知识库文档ID(knowledge_document.id)'", 'SELECT 1');
PREPARE s_feed_document_id FROM @ddl_feed_document_id;
EXECUTE s_feed_document_id;
DEALLOCATE PREPARE s_feed_document_id;

SET @col_feed_knowledge_points := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feed_task' AND COLUMN_NAME='knowledge_points');
SET @ddl_feed_knowledge_points := IF(@col_feed_knowledge_points=0, "ALTER TABLE `feed_task` ADD COLUMN `knowledge_points` INT DEFAULT 0 COMMENT '提取的知识点(分块)数量'", 'SELECT 1');
PREPARE s_feed_knowledge_points FROM @ddl_feed_knowledge_points;
EXECUTE s_feed_knowledge_points;
DEALLOCATE PREPARE s_feed_knowledge_points;
