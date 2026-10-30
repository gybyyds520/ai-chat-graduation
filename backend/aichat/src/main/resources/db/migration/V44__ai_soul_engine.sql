-- ============================================================
-- V44: AI 灵魂引擎（Soul Engine）
-- 用户的灵魂算法：
--   这一刻的自己(S) + 发生的经历(E) + 如何感受和理解(I) + 过去留下的记忆(M)
--   = 下一刻的自己(S')
-- 工程化：ai_soul_state（灵魂状态快照）+ ai_soul_experience（灵魂经历流水）
-- 说明：MySQL 8.0 不支持 CREATE TABLE IF NOT EXISTS 之外的幂等写法，
--       soul 表为全新表，直接创建即可（Flyway 版本保证只执行一次）。
-- ============================================================

CREATE TABLE IF NOT EXISTS `ai_soul_state` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '归属用户',
  `character_id` BIGINT NOT NULL COMMENT 'AI角色ID（灵魂载体）',
  `elf_id` BIGINT NULL COMMENT '关联AI精灵ID（可空，精灵可复用同一灵魂引擎）',
  `current_emotion` VARCHAR(32) NOT NULL DEFAULT 'neutral' COMMENT '当前情绪：neutral/happy/sad/angry/surprised/fearful/love',
  `mood_score` INT NOT NULL DEFAULT 60 COMMENT '心情值 0-100',
  `attitude_score` INT NOT NULL DEFAULT 30 COMMENT '对用户态度温度 -100~100（亲近度）',
  `personality_weights` TEXT NULL COMMENT '性格权重 JSON：{"温柔":0.8,"活泼":0.6,"理性":0.4,"依赖":0.5}',
  `core_beliefs` TEXT NULL COMMENT '核心信念/价值观摘要（自我理解的沉淀）',
  `self_summary` TEXT NULL COMMENT '此刻的自我描述（注入 prompt 用）',
  `version` INT NOT NULL DEFAULT 1 COMMENT '演化版本号（每次经历+1）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_soul_user_char` (`user_id`, `character_id`),
  KEY `idx_soul_elf` (`elf_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI灵魂状态快照';

CREATE TABLE IF NOT EXISTS `ai_soul_experience` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '归属用户',
  `character_id` BIGINT NOT NULL COMMENT 'AI角色ID',
  `elf_id` BIGINT NULL COMMENT '关联AI精灵ID（可空）',
  `experience` TEXT NOT NULL COMMENT '刚发生的经历（对话内容摘要）',
  `feeling` TEXT NULL COMMENT '如何感受（情绪反应文本）',
  `understanding` TEXT NULL COMMENT '如何理解（认知评价文本）',
  `memory_ref` TEXT NULL COMMENT '沉淀的记忆内容（写入 ai_memory 的摘要）',
  `impact` TEXT NULL COMMENT '对下一刻自己的影响摘要',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '经历发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_soul_exp_user_char` (`user_id`, `character_id`),
  KEY `idx_soul_exp_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI灵魂经历流水';
