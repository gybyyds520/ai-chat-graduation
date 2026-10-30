package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiCharacter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiCharacterMapper extends BaseMapper<AiCharacter> {
}