-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: ai_chat
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `access_log`
--

DROP TABLE IF EXISTS `access_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `access_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ai_system_id` bigint DEFAULT NULL,
  `data_id` bigint DEFAULT NULL,
  `access_type` enum('read','write','delete','query','upload','download') NOT NULL,
  `request_method` varchar(10) DEFAULT NULL,
  `request_path` varchar(500) DEFAULT NULL,
  `request_params` text,
  `response_status` int DEFAULT NULL,
  `response_time` int DEFAULT NULL,
  `ip_address` varchar(50) DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `error_message` text,
  `accessed_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_access_log_accessed_at` (`accessed_at`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `advertisement`
--

DROP TABLE IF EXISTS `advertisement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `advertisement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) DEFAULT NULL,
  `content` text,
  `link_url` varchar(500) DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `position` varchar(50) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `priority` int DEFAULT '0',
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `click_count` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_by` bigint DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_character`
--

DROP TABLE IF EXISTS `ai_character`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_character` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `prompt` text NOT NULL,
  `is_system` tinyint(1) DEFAULT '0',
  `model` varchar(100) DEFAULT 'phi3:mini' COMMENT '妯″瀷鍚嶇О',
  `user_id` bigint DEFAULT NULL COMMENT '鐢ㄦ埛ID',
  `is_shared` tinyint(1) DEFAULT '0' COMMENT '鏄惁鍒嗕韩',
  `personality` text,
  `voice_style` varchar(100) DEFAULT NULL,
  `emotion_base` varchar(100) DEFAULT NULL,
  `expressions` text,
  `actions` text,
  `backstory` text,
  `relationship` varchar(100) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL COMMENT '创建者ID',
  `updated_by` bigint DEFAULT NULL COMMENT '最后更新者ID',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_elf`
--

DROP TABLE IF EXISTS `ai_elf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_elf` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `name` varchar(100) DEFAULT NULL,
  `theme` varchar(50) DEFAULT NULL,
  `level` int DEFAULT '1',
  `experience` int DEFAULT '0',
  `energy` int DEFAULT '100',
  `health` int DEFAULT '100',
  `mood` int DEFAULT '50',
  `avatar` varchar(255) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_elf_alarm`
--

DROP TABLE IF EXISTS `ai_elf_alarm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_elf_alarm` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `elf_id` bigint NOT NULL COMMENT '精灵ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `alarm_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提醒时间',
  `repeat_days` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '重复日期',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否激活',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_elf_id` (`elf_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精灵闹钟表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_elf_focus`
--

DROP TABLE IF EXISTS `ai_elf_focus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_elf_focus` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `elf_id` bigint NOT NULL COMMENT '精灵ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `focus_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专注类型',
  `focus_start_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '开始时间',
  `focus_end_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结束时间',
  `focus_duration` int DEFAULT '0' COMMENT '专注时长(分钟)',
  `is_completed` tinyint(1) DEFAULT '0' COMMENT '是否完成',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_elf_id` (`elf_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精灵专注表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_elf_health_reminder`
--

DROP TABLE IF EXISTS `ai_elf_health_reminder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_elf_health_reminder` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `elf_id` bigint NOT NULL COMMENT '精灵ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `reminder_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提醒类型',
  `reminder_message` text COLLATE utf8mb4_unicode_ci COMMENT '提醒消息',
  `reminder_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提醒时间',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否激活',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_elf_id` (`elf_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精灵健康提醒表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_elf_schedule`
--

DROP TABLE IF EXISTS `ai_elf_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_elf_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `elf_id` bigint NOT NULL COMMENT '精灵ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `start_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '开始时间',
  `end_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结束时间',
  `is_completed` tinyint(1) DEFAULT '0' COMMENT '是否完成',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_elf_id` (`elf_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精灵日程表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_elf_sleep`
--

DROP TABLE IF EXISTS `ai_elf_sleep`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_elf_sleep` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `elf_id` bigint NOT NULL COMMENT '精灵ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `sleep_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入睡时间',
  `wake_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '醒来时间',
  `quality` int DEFAULT '0' COMMENT '睡眠质量(1-10)',
  `duration_hours` double DEFAULT '0' COMMENT '睡眠时长(小时)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_elf_id` (`elf_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精灵睡眠记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_model`
--

DROP TABLE IF EXISTS `ai_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `model_name` varchar(100) DEFAULT NULL,
  `model_key` varchar(100) DEFAULT NULL,
  `provider` varchar(50) DEFAULT NULL,
  `description` text,
  `cost_per_token` double DEFAULT NULL,
  `max_tokens` int DEFAULT NULL,
  `available` tinyint(1) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `priority` int DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_key` (`model_key`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_system`
--

DROP TABLE IF EXISTS `ai_system`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_system` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `system_name` varchar(100) NOT NULL,
  `system_type` varchar(50) NOT NULL,
  `api_key` varchar(255) NOT NULL,
  `secret_key` varchar(255) NOT NULL,
  `description` text,
  `status` tinyint DEFAULT '1',
  `rate_limit` int DEFAULT '100',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_access_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `system_name` (`system_name`),
  UNIQUE KEY `api_key` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `announcement`
--

DROP TABLE IF EXISTS `announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告内容',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED',
  `priority` int DEFAULT '1' COMMENT '优先级: 0-低, 1-中, 2-高, 3-紧急',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `display_scope` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ALL' COMMENT '显示范围: ALL/LOGGED_IN/ADMIN',
  `author` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作者',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`),
  KEY `idx_display_scope` (`display_scope`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `character_avatar_template`
--

DROP TABLE IF EXISTS `character_avatar_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `character_avatar_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `style` varchar(50) DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `is_system` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `character_emotion_log`
--

DROP TABLE IF EXISTS `character_emotion_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `character_emotion_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `character_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `emotion_type` varchar(50) DEFAULT NULL,
  `emotion_intensity` double DEFAULT NULL,
  `trigger_message` text,
  `response_text` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `character_interaction`
--

DROP TABLE IF EXISTS `character_interaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `character_interaction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `character_id` bigint NOT NULL COMMENT '角色ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `interaction_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '交互类型',
  `interaction_count` int DEFAULT '0' COMMENT '交互次数',
  `last_interaction_time` datetime DEFAULT NULL COMMENT '最后交互时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_character_id` (`character_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色交互表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `character_memory`
--

DROP TABLE IF EXISTS `character_memory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `character_memory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `character_id` bigint NOT NULL COMMENT '角色ID',
  `memory_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '记忆键',
  `memory_value` text COLLATE utf8mb4_unicode_ci COMMENT '记忆值',
  `memory_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '记忆类型',
  `importance` int DEFAULT '1' COMMENT '重要程度',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_character_id` (`character_id`),
  KEY `idx_memory_key` (`memory_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色记忆表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `character_preference`
--

DROP TABLE IF EXISTS `character_preference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `character_preference` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `character_id` bigint NOT NULL COMMENT '角色ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `preference_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '偏好键',
  `preference_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '偏好值',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_character_id` (`character_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色偏好表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `character_share`
--

DROP TABLE IF EXISTS `character_share`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `character_share` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `character_id` bigint NOT NULL COMMENT '角色ID',
  `from_user_id` bigint DEFAULT NULL COMMENT '分享者ID',
  `to_user_id` bigint DEFAULT NULL COMMENT '被分享者ID',
  `permissions` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'read' COMMENT '权限',
  `shared_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '分享时间',
  PRIMARY KEY (`id`),
  KEY `idx_character_id` (`character_id`),
  KEY `idx_to_user_id` (`to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色分享表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_file`
--

DROP TABLE IF EXISTS `chat_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(100) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `character_id` bigint DEFAULT NULL,
  `file_name` varchar(200) DEFAULT NULL,
  `file_url` varchar(500) DEFAULT NULL,
  `file_type` varchar(50) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `extracted_content` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_image`
--

DROP TABLE IF EXISTS `chat_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(100) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `character_id` bigint DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `ocr_result` text,
  `image_description` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_message`
--

DROP TABLE IF EXISTS `chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `character_id` bigint DEFAULT NULL,
  `sender_type` varchar(50) NOT NULL,
  `content` text NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_chat_msg_session_deleted_time` (`session_id`,`is_deleted`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1652 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `community_activity`
--

DROP TABLE IF EXISTS `community_activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `community_id` bigint DEFAULT NULL,
  `activity_name` varchar(200) DEFAULT NULL,
  `activity_description` text,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `organizer_id` bigint DEFAULT NULL,
  `participant_count` int DEFAULT '0',
  `status` varchar(20) DEFAULT 'UPCOMING',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `community_activity_participant`
--

DROP TABLE IF EXISTS `community_activity_participant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_activity_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `community_member`
--

DROP TABLE IF EXISTS `community_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `community_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `role` varchar(20) DEFAULT 'member',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `community_post`
--

DROP TABLE IF EXISTS `community_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `community_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `user_name` varchar(100) DEFAULT NULL,
  `content` text,
  `like_count` int DEFAULT '0',
  `comment_count` int DEFAULT '0',
  `is_deleted` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `conversation_context`
--

DROP TABLE IF EXISTS `conversation_context`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversation_context` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `conversation_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `context_data` text COLLATE utf8mb4_unicode_ci COMMENT '上下文数据(JSON)',
  `context_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上下文类型',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL,
  `updated_by` bigint DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话标识（与 conversation_id 互补）',
  `character_id` bigint DEFAULT NULL COMMENT 'AI 角色 ID',
  `context_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上下文键',
  `context_value` text COLLATE utf8mb4_unicode_ci COMMENT '上下文值',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（NULL 表示永久）',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_conv_user_deleted_created` (`user_id`,`is_deleted`,`created_at`),
  KEY `idx_conv_session_expire` (`session_id`,`expire_time`),
  KEY `idx_conv_user_character_expire` (`user_id`,`character_id`,`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话上下文表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `conversation_context_summary`
--

DROP TABLE IF EXISTS `conversation_context_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversation_context_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(100) DEFAULT NULL,
  `compressed_context` text,
  `key_points` text,
  `topic_summary` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `conversation_summary`
--

DROP TABLE IF EXISTS `conversation_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversation_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `conversation_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `summary` text COLLATE utf8mb4_unicode_ci COMMENT '会话摘要',
  `keywords` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关键词',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话摘要表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_access_permission`
--

DROP TABLE IF EXISTS `data_access_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_access_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `data_id` bigint DEFAULT NULL,
  `ai_system_id` bigint DEFAULT NULL,
  `permission_type` enum('read','write','delete','admin') DEFAULT 'read',
  `data_category` varchar(100) DEFAULT NULL,
  `expires_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_category`
--

DROP TABLE IF EXISTS `data_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_name` varchar(100) NOT NULL,
  `category_code` varchar(50) NOT NULL,
  `description` text,
  `parent_id` bigint DEFAULT NULL,
  `icon` varchar(100) DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `status` tinyint DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `category_name` (`category_name`),
  UNIQUE KEY `category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_import_task`
--

DROP TABLE IF EXISTS `data_import_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_import_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_name` varchar(255) NOT NULL,
  `import_type` enum('api','file','manual','batch') NOT NULL,
  `file_path` varchar(500) DEFAULT NULL,
  `file_format` varchar(20) DEFAULT NULL,
  `total_count` int DEFAULT '0',
  `success_count` int DEFAULT '0',
  `fail_count` int DEFAULT '0',
  `status` enum('pending','processing','completed','failed') DEFAULT 'pending',
  `error_log` text,
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_pool`
--

DROP TABLE IF EXISTS `data_pool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_pool` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `content` longtext NOT NULL,
  `data_type` varchar(50) NOT NULL,
  `format` varchar(20) NOT NULL,
  `file_path` varchar(500) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `checksum` varchar(64) DEFAULT NULL,
  `category` varchar(100) DEFAULT NULL,
  `description` text,
  `metadata` json DEFAULT NULL,
  `version` int DEFAULT '1',
  `is_public` tinyint DEFAULT '0',
  `status` tinyint DEFAULT '1',
  `created_by` varchar(100) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `expires_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_tag`
--

DROP TABLE IF EXISTS `data_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tag_name` varchar(100) NOT NULL,
  `tag_type` varchar(50) DEFAULT 'general',
  `description` varchar(500) DEFAULT NULL,
  `color` varchar(20) DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_tag_relation`
--

DROP TABLE IF EXISTS `data_tag_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_tag_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `data_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_tag` (`data_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_version`
--

DROP TABLE IF EXISTS `data_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_version` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `data_id` bigint NOT NULL,
  `version` int NOT NULL,
  `content` longtext,
  `change_summary` text,
  `changed_by` varchar(100) DEFAULT NULL,
  `changed_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_version` (`data_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `elf_voice_command`
--

DROP TABLE IF EXISTS `elf_voice_command`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `elf_voice_command` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `elf_id` bigint NOT NULL COMMENT '精灵ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `command_text` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '命令文本',
  `command_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '命令类型',
  `is_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_elf_id` (`elf_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精灵语音命令表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `friend_chat_message`
--

DROP TABLE IF EXISTS `friend_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL,
  `receiver_id` bigint NOT NULL,
  `content` text NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `status` int DEFAULT '0',
  `type` varchar(50) DEFAULT 'text' COMMENT '娑堟伅绫诲瀷(text/image/file/audio/video)',
  PRIMARY KEY (`id`),
  KEY `idx_sender` (`sender_id`),
  KEY `idx_receiver` (`receiver_id`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `friend_group`
--

DROP TABLE IF EXISTS `friend_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组名称',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友分组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `friend_group_member`
--

DROP TABLE IF EXISTS `friend_group_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend_group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` bigint NOT NULL COMMENT '分组ID',
  `friend_id` bigint NOT NULL COMMENT '好友ID',
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友分组成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `friendship`
--

DROP TABLE IF EXISTS `friendship`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friendship` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `friend_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `accept_time` datetime DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_friend` (`user_id`,`friend_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_friend_id` (`friend_id`),
  KEY `idx_friendship_user_status` (`user_id`,`status`),
  KEY `idx_friendship_friend_status` (`friend_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=93 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `group_bot`
--

DROP TABLE IF EXISTS `group_bot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_bot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint DEFAULT NULL,
  `bot_name` varchar(100) DEFAULT NULL,
  `bot_description` varchar(500) DEFAULT NULL,
  `enabled` tinyint(1) DEFAULT '1',
  `trigger_keywords` text,
  `auto_reply_rules` json DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `group_bot_message`
--

DROP TABLE IF EXISTS `group_bot_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_bot_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bot_id` bigint DEFAULT NULL,
  `group_id` bigint DEFAULT NULL,
  `message_content` text,
  `trigger_type` varchar(50) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `group_chat`
--

DROP TABLE IF EXISTS `group_chat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '群名称',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '群描述',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '群头像',
  `creator_id` bigint NOT NULL COMMENT '创建者ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` int DEFAULT '1' COMMENT '状态(0-禁用, 1-启用)',
  `announcement` text COLLATE utf8mb4_unicode_ci,
  `settings` json DEFAULT NULL,
  `activity_score` int DEFAULT '0',
  `last_activity_time` datetime DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `updated_by` bigint DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `group_chat_member`
--

DROP TABLE IF EXISTS `group_chat_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_chat_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` bigint NOT NULL COMMENT '群ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'member' COMMENT '角色(admin/member)',
  `join_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `mute_until` datetime DEFAULT NULL COMMENT '禁言到期时间',
  `status` int DEFAULT '1' COMMENT '状态(0-退出, 1-正常)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`,`user_id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_group_member_group_status` (`group_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `group_chat_message`
--

DROP TABLE IF EXISTS `group_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` bigint NOT NULL COMMENT '群ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `sender_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发送者名称',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `message_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'text' COMMENT '消息类型(text/emoji/file)',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件名',
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `extra_data` text COLLATE utf8mb4_unicode_ci COMMENT '扩展数据(JSON格式)',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `status` int DEFAULT '0' COMMENT '状态(0-未读, 1-已读)',
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `group_member`
--

DROP TABLE IF EXISTS `group_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` bigint NOT NULL COMMENT '群ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'member' COMMENT '角色',
  `join_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `leave_time` datetime DEFAULT NULL COMMENT '离开时间',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否活跃',
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表(冗余)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `group_message`
--

DROP TABLE IF EXISTS `group_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` bigint NOT NULL COMMENT '群ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `message_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'text' COMMENT '消息类型',
  `reply_to_message_id` bigint DEFAULT NULL COMMENT '回复消息ID',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息表(冗余)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `health_analysis_record`
--

DROP TABLE IF EXISTS `health_analysis_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `health_analysis_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `elf_id` bigint NOT NULL COMMENT '精灵ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `analysis_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分析类型',
  `analysis_data` text COLLATE utf8mb4_unicode_ci COMMENT '分析数据(JSON)',
  `analysis_score` int DEFAULT '0' COMMENT '分析评分',
  `recommendations` text COLLATE utf8mb4_unicode_ci COMMENT '建议',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_elf_id` (`elf_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_analysis_type` (`analysis_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康分析记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `interest_community`
--

DROP TABLE IF EXISTS `interest_community`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `interest_community` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL,
  `description` text,
  `avatar` varchar(255) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `member_count` int DEFAULT '0',
  `post_count` int DEFAULT '0',
  `visibility` varchar(20) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `login_alert`
--

DROP TABLE IF EXISTS `login_alert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_alert` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `alert_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '告警类型',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `location` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地理位置',
  `device_info` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备信息',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录告警表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `login_device`
--

DROP TABLE IF EXISTS `login_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `device_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备ID',
  `device_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备名称',
  `device_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备类型',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `location` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地理位置',
  `is_trusted` tinyint(1) DEFAULT '0' COMMENT '是否信任',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `login_risk_assessment`
--

DROP TABLE IF EXISTS `login_risk_assessment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_risk_assessment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `login_time` datetime DEFAULT NULL,
  `ip_address` varchar(50) DEFAULT NULL,
  `device_id` varchar(100) DEFAULT NULL,
  `location` varchar(100) DEFAULT NULL,
  `risk_score` int DEFAULT '0',
  `risk_level` varchar(20) DEFAULT 'LOW',
  `assessment_reason` varchar(500) DEFAULT NULL,
  `action_taken` varchar(50) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,
  `target_type` varchar(50) DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `actor_id` bigint DEFAULT NULL,
  `actor_name` varchar(100) DEFAULT NULL,
  `message` text,
  `is_read` tinyint(1) DEFAULT '0',
  `is_deleted` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oauth_provider`
--

DROP TABLE IF EXISTS `oauth_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oauth_provider` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `provider` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提供商用户ID',
  `access_token` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '访问令牌',
  `refresh_token` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '刷新令牌',
  `expire_time` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `provider_user_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_user` (`provider`,`provider_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_provider_name` (`provider`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth登录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oauth_user`
--

DROP TABLE IF EXISTS `oauth_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oauth_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `provider` varchar(50) NOT NULL,
  `provider_user_id` varchar(100) NOT NULL,
  `access_token` varchar(500) DEFAULT NULL,
  `refresh_token` varchar(500) DEFAULT NULL,
  `token_expire_time` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_user` (`provider`,`provider_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pay_account`
--

DROP TABLE IF EXISTS `pay_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pay_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `account_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '账户类型',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '余额',
  `freeze_amount` decimal(10,2) DEFAULT '0.00' COMMENT '冻结金额',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_account_type` (`account_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付账户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_record`
--

DROP TABLE IF EXISTS `payment_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `payment_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UPGRADE' COMMENT '支付类型: UPGRADE/RENEW',
  `version_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '版本类型: PRO/ENTERPRISE',
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `duration_days` int NOT NULL COMMENT '购买天数',
  `payment_method` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付方式',
  `transaction_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '交易编号',
  `payment_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '支付状态: PENDING/SUCCESS/FAILED',
  `review_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '审核状态: PENDING/APPROVED/REJECTED',
  `review_remark` text COLLATE utf8mb4_unicode_ci COMMENT '审核备注',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
  `payment_time` datetime DEFAULT NULL COMMENT '支付时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_by` bigint DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `transaction_id` (`transaction_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_version_type` (`version_type`),
  KEY `idx_payment_status` (`payment_status`),
  KEY `idx_review_status` (`review_status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_payment_user_created` (`user_id`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `privacy_setting`
--

DROP TABLE IF EXISTS `privacy_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `privacy_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `setting_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设置项',
  `setting_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设置值',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_setting_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='隐私设置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sleep_analysis_report`
--

DROP TABLE IF EXISTS `sleep_analysis_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sleep_analysis_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `elf_id` bigint NOT NULL COMMENT '精灵ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `report_date` date DEFAULT NULL COMMENT '报告日期',
  `total_sleep_hours` double DEFAULT '0' COMMENT '总睡眠时长',
  `deep_sleep_hours` double DEFAULT '0' COMMENT '深度睡眠时长',
  `sleep_efficiency` double DEFAULT '0' COMMENT '睡眠效率',
  `sleep_score` int DEFAULT '0' COMMENT '睡眠评分',
  `suggestions` text COLLATE utf8mb4_unicode_ci COMMENT '改善建议',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_elf_id` (`elf_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_report_date` (`report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='睡眠分析报告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sms_code`
--

DROP TABLE IF EXISTS `sms_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sms_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号',
  `code` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '验证码',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型(register/login/reset)',
  `expires_at` datetime NOT NULL COMMENT '过期时间',
  `is_used` tinyint(1) DEFAULT '0' COMMENT '是否使用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信验证码表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `social_comment`
--

DROP TABLE IF EXISTS `social_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `social_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '评论内容',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_comment_post_deleted_created` (`post_id`,`is_deleted`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `social_like`
--

DROP TABLE IF EXISTS `social_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `social_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `social_post`
--

DROP TABLE IF EXISTS `social_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `social_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `user_name` varchar(100) DEFAULT NULL,
  `content` text,
  `media_type` varchar(50) DEFAULT NULL,
  `media_urls` text,
  `video_url` varchar(500) DEFAULT NULL,
  `video_cover` varchar(500) DEFAULT NULL,
  `location` varchar(200) DEFAULT NULL,
  `visibility` varchar(20) DEFAULT NULL,
  `like_count` int DEFAULT '0',
  `comment_count` int DEFAULT '0',
  `share_count` int DEFAULT '0',
  `is_deleted` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_post_user_deleted_created` (`user_id`,`is_deleted`,`created_at`),
  KEY `idx_post_deleted_created` (`is_deleted`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `social_post_tag`
--

DROP TABLE IF EXISTS `social_post_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `social_post_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `tag_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_tag_name` (`tag_name`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子标签表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `social_recommendation`
--

DROP TABLE IF EXISTS `social_recommendation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `social_recommendation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `recommended_user_id` bigint DEFAULT NULL,
  `recommendation_type` varchar(50) DEFAULT NULL,
  `similarity_score` double DEFAULT NULL,
  `common_interests` text,
  `common_friends` int DEFAULT '0',
  `interaction_count` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `social_share`
--

DROP TABLE IF EXISTS `social_share`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `social_share` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `share_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分享类型',
  `share_count` int DEFAULT '0' COMMENT '分享次数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社交分享表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `nickname` varchar(50) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `version_type` varchar(20) DEFAULT 'FREE' COMMENT '版本类型: FREE/PRO/ENTERPRISE',
  `version_expire_time` datetime DEFAULT NULL COMMENT '版本有效期截止时间',
  `account_status` varchar(20) DEFAULT 'ACTIVE' COMMENT '账户状态: ACTIVE/INACTIVE/SUSPENDED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `email` varchar(255) DEFAULT NULL COMMENT '閭',
  `bio` text COMMENT '涓汉绠�浠�',
  `avatar_url` varchar(500) DEFAULT NULL,
  `interests` varchar(500) DEFAULT NULL,
  `location` varchar(100) DEFAULT NULL,
  `website` varchar(200) DEFAULT NULL,
  `gender` varchar(10) DEFAULT NULL,
  `birthday` varchar(20) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `phone_verified` tinyint(1) DEFAULT NULL,
  `email_verified` tinyint(1) DEFAULT NULL,
  `mfa_enabled` tinyint(1) DEFAULT NULL,
  `mfa_secret` varchar(100) DEFAULT NULL,
  `last_login_ip` varchar(50) DEFAULT NULL,
  `last_login_time` datetime DEFAULT NULL,
  `failed_login_count` int DEFAULT NULL,
  `lock_time` datetime DEFAULT NULL,
  `security_question` varchar(200) DEFAULT NULL,
  `security_answer` varchar(200) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL COMMENT '创建者ID（注册时自填）',
  `updated_by` bigint DEFAULT NULL COMMENT '最后更新者ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_version_type` (`version_type`),
  KEY `idx_version_expire_time` (`version_expire_time`),
  KEY `idx_account_status` (`account_status`)
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_account`
--

DROP TABLE IF EXISTS `user_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_account` (
  `user_id` bigint NOT NULL COMMENT '关联 user.id',
  `version_type` varchar(20) DEFAULT 'FREE' COMMENT '订阅类型（FREE/PREMIUM/ENTERPRISE）',
  `version_expire_time` datetime DEFAULT NULL COMMENT '订阅到期时间',
  `account_status` varchar(20) DEFAULT 'ACTIVE' COMMENT '账户状态（ACTIVE/LOCKED/DELETED/SUSPENDED）',
  `phone_verified` tinyint(1) DEFAULT '0' COMMENT '手机是否验证',
  `email_verified` tinyint(1) DEFAULT '0' COMMENT '邮箱是否验证',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  KEY `idx_user_account_status` (`account_status`),
  KEY `idx_user_account_version` (`version_type`,`version_expire_time`),
  KEY `idx_user_account_status_version` (`account_status`,`version_type`),
  CONSTRAINT `fk_user_account_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账户状态表（订阅与状态）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_auto_save`
--

DROP TABLE IF EXISTS `user_auto_save`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_auto_save` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID',
  `data_type` varchar(50) NOT NULL COMMENT '鏁版嵁绫诲瀷(form/editor/chat/record)',
  `data_content` text COMMENT '淇濆瓨鐨勬暟鎹唴瀹�',
  `session_id` varchar(100) DEFAULT NULL COMMENT '浼氳瘽ID',
  `page_url` varchar(500) DEFAULT NULL COMMENT '椤甸潰URL',
  `save_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '淇濆瓨鏃堕棿',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_data_type` (`data_type`),
  KEY `idx_save_time` (`save_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢ㄦ埛鑷姩淇濆瓨琛�';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_badge`
--

DROP TABLE IF EXISTS `user_badge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_badge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `badge_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '徽章名称',
  `badge_level` int DEFAULT '1' COMMENT '徽章等级',
  `earned_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_badge_name` (`badge_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户徽章表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_block`
--

DROP TABLE IF EXISTS `user_block`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_block` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `block_id` bigint NOT NULL COMMENT '被拉黑ID',
  `reason` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原因',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_block_id` (`block_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户拉黑表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_data_record`
--

DROP TABLE IF EXISTS `user_data_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_data_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID',
  `data_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鏁版嵁绫诲瀷(form/chat_message/user_action/editor_content)',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '鏁版嵁鍐呭',
  `metadata` text COLLATE utf8mb4_unicode_ci COMMENT '鍏冩暟鎹�(JSON鏍煎紡)',
  `tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鏍囩(閫楀彿鍒嗛殧)',
  `integrity_score` double DEFAULT '0.5' COMMENT '瀹屾暣鎬ц瘎鍒�(0-1)',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING_VALIDATION' COMMENT '鐘舵��(PENDING_VALIDATION/VALIDATED/INVALID)',
  `capture_time` datetime DEFAULT NULL COMMENT '鎹曡幏鏃堕棿',
  `group_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍒嗙粍ID',
  `backup_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING_BACKUP' COMMENT '澶囦唤鐘舵��(PENDING_BACKUP/BACKED_UP/FAILED)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_data_type` (`data_type`),
  KEY `idx_capture_time` (`capture_time`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐢ㄦ埛鏁版嵁璁板綍琛�';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_follow`
--

DROP TABLE IF EXISTS `user_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `follow_id` bigint NOT NULL COMMENT '关注ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`,`follow_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_follow_id` (`follow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_interest`
--

DROP TABLE IF EXISTS `user_interest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_interest` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `interest_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '兴趣名称',
  `interest_level` int DEFAULT '1' COMMENT '兴趣程度',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_interest_name` (`interest_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户兴趣表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_online_status`
--

DROP TABLE IF EXISTS `user_online_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_online_status` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `is_online` tinyint(1) DEFAULT '0' COMMENT '是否在线',
  `last_active_time` datetime DEFAULT NULL COMMENT '最后活跃时间',
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'offline' COMMENT '在线状态：online/offline/away/busy',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后登录 IP',
  `device_info` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备信息',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_online` (`is_online`),
  KEY `idx_user_online_status_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户在线状态表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_report`
--

DROP TABLE IF EXISTS `user_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reporter_id` bigint NOT NULL COMMENT '举报者ID',
  `reported_id` bigint NOT NULL COMMENT '被举报者ID',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '举报类型',
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '举报原因',
  `evidence` text COLLATE utf8mb4_unicode_ci COMMENT '证据',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_reported_id` (`reported_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户举报表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_security`
--

DROP TABLE IF EXISTS `user_security`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_security` (
  `user_id` bigint NOT NULL COMMENT '关联 user.id',
  `password_hash` varchar(100) NOT NULL COMMENT 'BCrypt 加密后的密码哈希',
  `mfa_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用 MFA',
  `mfa_secret` varchar(100) DEFAULT NULL COMMENT 'MFA 密钥（加密存储）',
  `failed_login_count` int DEFAULT '0' COMMENT '登录失败次数',
  `lock_time` datetime DEFAULT NULL COMMENT '账户锁定时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最近登录 IP',
  `last_login_time` datetime DEFAULT NULL COMMENT '最近登录时间',
  `security_question` varchar(200) DEFAULT NULL COMMENT '密保问题（加密存储）',
  `security_answer` varchar(200) DEFAULT NULL COMMENT '密保答案（加密存储）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  KEY `idx_user_security_last_login` (`last_login_time`),
  CONSTRAINT `fk_user_security_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户安全信息表（敏感字段隔离）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_statistics`
--

DROP TABLE IF EXISTS `user_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_statistics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_chats` int DEFAULT '0' COMMENT '总聊天次数',
  `total_messages` int DEFAULT '0' COMMENT '总消息数',
  `total_tokens` int DEFAULT '0' COMMENT '总token数',
  `total_time_spent` int DEFAULT '0' COMMENT '总耗时(分钟)',
  `last_active_date` date DEFAULT NULL COMMENT '最后活跃日期',
  `streak_days` int DEFAULT '0' COMMENT '连续活跃天数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户统计表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_tag`
--

DROP TABLE IF EXISTS `user_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tag_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户标签表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_view_record`
--

DROP TABLE IF EXISTS `user_view_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_view_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `view_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览类型',
  `view_id` bigint DEFAULT NULL COMMENT '浏览ID',
  `view_count` int DEFAULT '1' COMMENT '浏览次数',
  `last_view_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后浏览时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_view_type` (`view_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户浏览记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'ai_chat'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-20 19:01:02

