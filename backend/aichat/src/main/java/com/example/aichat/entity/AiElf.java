package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_elf")
public class AiElf {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String name;
    
    private String avatar;
    
    private String theme;
    
    private String personality;
    
    private Integer level;
    
    private Integer experience;
    
    private Integer focusCount;
    
    private Integer totalFocusTime;
    
    private Integer healthPoints;

    /** 好感度 0-100，默认 0（AI 精灵养成深化） */
    private Integer affection;

    /** 关系等级 1-10，默认 1 */
    private Integer relationshipLevel;

    /** 亲密度 0-1000，默认 0 */
    private Integer intimacy;

    /** 心情值 0-100，默认 80（桌面养成 V40） */
    private Integer mood;

    /** 饱腹度 0-100，默认 70（桌面养成 V40） */
    private Integer fullness;

    /** 最后互动时间 */
    private LocalDateTime lastInteractAt;

    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}