-- V43: 每日签到打卡表 + user_quota 签到奖励额度列
--
-- 1. user_checkin：user_id + checkin_date 唯一，记录连续天数与本次奖励
-- 2. user_quota 增加 bonus_limit：签到等活动叠加在会员等级上限之上的当日额度
-- 说明：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，沿用 information_schema 动态 DDL，幂等可重复执行。

CREATE TABLE IF NOT EXISTS user_checkin (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '签到用户 ID',
    checkin_date DATE NOT NULL COMMENT '签到自然日',
    consecutive_days INT NOT NULL DEFAULT 1 COMMENT '截至当天连续签到天数',
    reward_granted INT NOT NULL DEFAULT 0 COMMENT '本次签到发放的额度奖励',
    created_at DATETIME NULL COMMENT '签到时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_checkin_date (user_id, checkin_date),
    KEY idx_checkin_user_date (user_id, checkin_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日签到记录';

-- user_quota.bonus_limit：活动奖励叠加额度（默认 0）
SET @has_bonus := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_quota' AND COLUMN_NAME = 'bonus_limit');
SET @ddl_bonus := IF(@has_bonus = 0, 'ALTER TABLE user_quota ADD COLUMN bonus_limit INT NOT NULL DEFAULT 0 COMMENT ''签到等活动叠加的当日额度''', 'SELECT 1');
PREPARE s_bonus FROM @ddl_bonus;
EXECUTE s_bonus;
DEALLOCATE PREPARE s_bonus;
