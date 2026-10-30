package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到打卡记录。
 *
 * 每个用户每天最多一条：user_id + checkin_date 唯一。
 * consecutiveDays 记录签到当天的连续签到天数（断签后重置为 1）。
 * rewardGranted 记录本次签到发放的额度奖励总额（免费用户基础 +5，满 7 天/30 天额外奖励）。
 */
@Data
@TableName("user_checkin")
public class UserCheckin {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 签到自然日。 */
    @TableField("checkin_date")
    private LocalDate checkinDate;

    /** 截至当天的连续签到天数。 */
    @TableField("consecutive_days")
    private Integer consecutiveDays;

    /** 本次签到发放的额度奖励（含连续里程碑额外奖励）。 */
    @TableField("reward_granted")
    private Integer rewardGranted;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
