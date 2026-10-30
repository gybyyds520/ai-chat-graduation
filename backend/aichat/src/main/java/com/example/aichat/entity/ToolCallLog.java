package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工具调用日志。
 */
@Data
@TableName("tool_call_log")
public class ToolCallLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String toolName;

    /** 入参（JSON 字符串）。 */
    private String parameters;

    /** 执行结果（JSON 字符串）。 */
    private String result;

    private Boolean success;

    private Long durationMs;

    private LocalDateTime createdAt;
}
