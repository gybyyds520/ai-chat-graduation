package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("elf_voice_command")
public class ElfVoiceCommand {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long elfId;
    
    private Long userId;
    
    private String commandText;
    
    private String commandType;
    
    private Boolean executed;
    
    private LocalDateTime createdAt;
}
