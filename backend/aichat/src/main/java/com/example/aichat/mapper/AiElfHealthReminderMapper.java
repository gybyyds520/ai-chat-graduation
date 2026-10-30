package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiElfHealthReminder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiElfHealthReminderMapper extends BaseMapper<AiElfHealthReminder> {
    
    @Select("SELECT * FROM ai_elf_health_reminder WHERE elf_id = #{elfId}")
    List<AiElfHealthReminder> getRemindersByElfId(@Param("elfId") Long elfId);
    
    @Select("SELECT * FROM ai_elf_health_reminder WHERE elf_id = #{elfId} AND reminder_type = #{reminderType}")
    AiElfHealthReminder findByType(@Param("elfId") Long elfId, @Param("reminderType") String reminderType);
}