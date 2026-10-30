package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiModelMapper extends BaseMapper<AiModel> {
    
    @Select("SELECT * FROM ai_model WHERE available = true ORDER BY priority")
    List<AiModel> getAvailableModels();
    
    @Select("SELECT * FROM ai_model WHERE category = #{category} AND available = true")
    List<AiModel> getModelsByCategory(@Param("category") String category);
    
    @Select("SELECT * FROM ai_model WHERE model_key = #{modelKey}")
    AiModel findByModelKey(@Param("modelKey") String modelKey);
}