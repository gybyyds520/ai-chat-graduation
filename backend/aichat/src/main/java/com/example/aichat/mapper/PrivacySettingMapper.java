package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.PrivacySetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrivacySettingMapper extends BaseMapper<PrivacySetting> {
    
    @Select("SELECT ps.* FROM privacy_setting ps WHERE ps.user_id = #{userId}")
    List<PrivacySetting> getSettingsByUserId(@Param("userId") Long userId);
    
    @Select("SELECT ps.* FROM privacy_setting ps WHERE ps.user_id = #{userId} AND ps.key = #{key}")
    PrivacySetting findByUserAndKey(@Param("userId") Long userId, @Param("key") String key);
}