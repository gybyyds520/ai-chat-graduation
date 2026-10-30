package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiElf;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiElfMapper extends BaseMapper<AiElf> {
    
    @Select("SELECT * FROM ai_elf WHERE user_id = #{userId}")
    AiElf findByUserId(@Param("userId") Long userId);
}