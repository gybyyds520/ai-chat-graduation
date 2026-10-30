package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_chat_member")
public class GroupChatMember {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("group_id")
    private Long groupId;
    
    @TableField("user_id")
    private Long userId;
    
    private String nickname;
    
    private String role;
    
    @TableField("join_time")
    private LocalDateTime joinTime;
    
    @TableField("mute_until")
    private LocalDateTime muteUntil;
    
    private Integer status;
}