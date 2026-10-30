package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_character")
public class AiCharacter {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    private String name;
    private String avatar;
    private String prompt;
    private String model;
    private String personality;
    private String voiceStyle;
    private String emotionBase;
    private String expressions;
    private String actions;
    private String backstory;
    private String relationship;
    @TableField("is_system")
    private Boolean isSystem;
    @TableField("is_shared")
    private Boolean isShared;

    // ===== 角色广场 / 发现页（A组）=====
    /** 使用次数（被用户开始对话时 +1）。 */
    @TableField("use_count")
    private Integer useCount;
    /** 平均评分（0-5）。 */
    private Double rating;

    // ===== 角色创建字段丰富化（B组）=====
    /** 角色一句话简介（广场卡片与详情展示）。 */
    private String description;
    /** 分类（广场分类筛选，如 治愈/知识/娱乐）。 */
    private String category;
    /** 开场白 / 第一句话。 */
    private String greeting;
    /** 对话示例（few-shot，文本或 JSON 片段）。 */
    @TableField("example_dialogue")
    private String exampleDialogue;
    /** 标签，逗号分隔。 */
    private String tags;
    /** 关联音色 ID（与多音色 TTS 联动）。 */
    @TableField("voice_id")
    private String voiceId;

    /** 角色启用的工具列表（逗号分隔的工具名，如 web_search,calculator；为空表示全部可用）。 */
    @TableField("enabled_tools")
    private String enabledTools;

    /** 头像生成风格（本地 SVG 预设：avataaars/botts/botts-neutral/jdenticon/gridy/micah）。未上传图片时按 name+style 确定性生成。 */
    @TableField("avatar_style")
    private String avatarStyle;

    @TableField(exist = false)
    private LocalDateTime createTime;
}