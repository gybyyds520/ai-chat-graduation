package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiElfSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiElfScheduleMapper extends BaseMapper<AiElfSchedule> {
    
    @Select("SELECT * FROM ai_elf_schedule WHERE elf_id = #{elfId} ORDER BY start_time")
    List<AiElfSchedule> getSchedulesByElfId(@Param("elfId") Long elfId);
}