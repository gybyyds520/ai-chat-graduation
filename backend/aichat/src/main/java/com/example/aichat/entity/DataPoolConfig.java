package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据池配置项（键值对持久化）。
 *
 * <p>存储 autoSaveEnabled / retentionDays / maxRecordsPerUser 等运行期开关，
 * 替代原 DataPoolService 中的 static 内存变量，使配置重启后保持、可被管理端修改。</p>
 */
@Data
@TableName("data_pool_config")
public class DataPoolConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
