package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiElfFocus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AiElfFocusMapper extends BaseMapper<AiElfFocus> {
    
    @Select("SELECT * FROM ai_elf_focus WHERE elf_id = #{elfId} ORDER BY start_time DESC")
    List<AiElfFocus> getFocusRecordsByElfId(@Param("elfId") Long elfId);
    
    @Select("SELECT SUM(duration_minutes) FROM ai_elf_focus WHERE elf_id = #{elfId} AND completed = true")
    Integer getTotalFocusMinutes(@Param("elfId") Long elfId);
    
    @Select("SELECT COUNT(*) FROM ai_elf_focus WHERE elf_id = #{elfId} AND completed = true")
    Integer getFocusCount(@Param("elfId") Long elfId);
    
    @Select("SELECT * FROM ai_elf_focus WHERE elf_id = #{elfId} AND completed = false ORDER BY start_time DESC LIMIT 1")
    AiElfFocus getCurrentFocus(@Param("elfId") Long elfId);
    
    @Select("SELECT COUNT(*) FROM ai_elf_focus WHERE elf_id = #{elfId} AND DATE(start_time) = #{date}")
    Integer getFocusCountByDate(@Param("elfId") Long elfId, @Param("date") LocalDate date);
}