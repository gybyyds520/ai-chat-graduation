package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * M1 ②：用户级定时提醒（星野式留存）。
 * 到点后由定时任务通过 WebSocket 推送给在线用户。
 */
@Data
@TableName("elf_reminder")
public class ElfReminder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 提醒标题/内容 */
    private String title;

    /** 提醒正文内容 */
    private String content;

    /** 触发时间 */
    @TableField("remind_time")
    private LocalDateTime remindTime;

    /** 是否已触发 */
    @TableField("triggered")
    private Boolean triggered;

    @TableField("create_time")
    private LocalDateTime createTime;
}
