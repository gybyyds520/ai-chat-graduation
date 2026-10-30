package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 全局配置项（键值对持久化）。
 *
 * <p>存储 defaultModel / temperature / maxTokens / requestTimeoutSec /
 * aiChatEnabled / maintenanceNotice 等运行期全局开关，
 * 模式参考 DataPoolConfig：键值对 + 进程内缓存，写后失效。</p>
 */
@Data
@TableName("ai_config")
public class AiConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
