package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.CharacterPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CharacterPreferenceMapper extends BaseMapper<CharacterPreference> {
    
    @Select("SELECT * FROM character_preference WHERE user_id = #{userId}")
    List<CharacterPreference> findByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM character_preference WHERE user_id = #{userId} AND character_id = #{characterId}")
    CharacterPreference findByUserIdAndCharacterId(@Param("userId") Long userId, @Param("characterId") Long characterId);
    
    @Select("SELECT character_id FROM character_preference WHERE user_id = #{userId} ORDER BY last_used_at DESC LIMIT #{limit}")
    List<Long> findRecentCharacterIds(@Param("userId") Long userId, @Param("limit") int limit);
    
    @Select("SELECT COUNT(*) > 0 FROM character_preference WHERE user_id = #{userId} AND character_id = #{characterId}")
    boolean existsByUserIdAndCharacterId(@Param("userId") Long userId, @Param("characterId") Long characterId);
}