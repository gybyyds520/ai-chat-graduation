package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户级通知设置（每个用户一行，upsert）。
 *
 * <p>控制该用户是否接收 like / comment / share / system 四类通知的推送。</p>
 */
@Data
@TableName("notification_setting")
public class NotificationSetting {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("like_enabled")
    private Boolean likeEnabled;

    @TableField("comment_enabled")
    private Boolean commentEnabled;

    @TableField("share_enabled")
    private Boolean shareEnabled;

    @TableField("system_enabled")
    private Boolean systemEnabled;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
