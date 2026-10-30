package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiElfAlarm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiElfAlarmMapper extends BaseMapper<AiElfAlarm> {
    
    @Select("SELECT * FROM ai_elf_alarm WHERE elf_id = #{elfId} ORDER BY time")
    List<AiElfAlarm> getAlarmsByElfId(@Param("elfId") Long elfId);
}