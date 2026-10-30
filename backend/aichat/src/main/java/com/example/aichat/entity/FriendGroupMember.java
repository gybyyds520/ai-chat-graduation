package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_group_member")
public class FriendGroupMember {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("group_id")
    private Long groupId;
    
    @TableField("friend_id")
    private Long friendId;
    
    private String remark;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}