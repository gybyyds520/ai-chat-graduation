package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户每日用量配额。
 *
 * 按 userId + featureType + resetDate 维度记录当日已用次数与上限，跨日自然重置。
 */
@Data
@TableName("user_quota")
public class UserQuota {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 功能类型：ai_chat / voice_call / image_gen / doc_upload。 */
    private String featureType;

    private Integer usedCount;

    private Integer limitCount;

    /** 签到等活动额外奖励的当日额度（叠加在会员等级上限之上，默认 0）。 */
    @TableField("bonus_limit")
    private Integer bonusLimit;

    /** 配额所属自然日（当日）。 */
    private LocalDate resetDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
