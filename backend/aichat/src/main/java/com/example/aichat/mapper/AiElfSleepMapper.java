package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiElfSleep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiElfSleepMapper extends BaseMapper<AiElfSleep> {
    
    @Select("SELECT * FROM ai_elf_sleep WHERE elf_id = #{elfId} ORDER BY record_date DESC")
    List<AiElfSleep> getSleepRecordsByElfId(@Param("elfId") Long elfId);
    
    @Select("SELECT AVG(duration_hours) FROM ai_elf_sleep WHERE elf_id = #{elfId} AND record_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)")
    Double getAverageSleepDuration(@Param("elfId") Long elfId);
    
    @Select("SELECT AVG(quality) FROM ai_elf_sleep WHERE elf_id = #{elfId} AND record_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)")
    Double getAverageSleepQuality(@Param("elfId") Long elfId);
}