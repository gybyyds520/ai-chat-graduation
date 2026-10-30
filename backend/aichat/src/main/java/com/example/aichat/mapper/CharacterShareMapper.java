package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.CharacterShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CharacterShareMapper extends BaseMapper<CharacterShare> {
    
    @Select("SELECT character_id FROM character_share WHERE shared_to_user_id = #{userId} AND status = 'ACTIVE'")
    List<Long> findSharedCharacterIdsByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM character_share WHERE character_id = #{characterId} AND status = 'ACTIVE'")
    List<CharacterShare> findByCharacterId(@Param("characterId") Long characterId);
    
    @Select("SELECT * FROM character_share WHERE shared_by_user_id = #{userId} AND status = 'ACTIVE'")
    List<CharacterShare> findSharedByUser(@Param("userId") Long userId);
    
    @Select("SELECT * FROM character_share WHERE shared_to_user_id = #{userId} AND status = 'ACTIVE'")
    List<CharacterShare> findSharedToUser(@Param("userId") Long userId);
    
    @Select("SELECT COUNT(*) > 0 FROM character_share WHERE character_id = #{characterId} AND shared_to_user_id = #{userId} AND status = 'ACTIVE'")
    boolean existsByCharacterIdAndUserId(@Param("characterId") Long characterId, @Param("userId") Long userId);
    
    @Select("SELECT permissions FROM character_share WHERE character_id = #{characterId} AND shared_to_user_id = #{userId} AND status = 'ACTIVE'")
    String findPermissionsByCharacterIdAndUserId(@Param("characterId") Long characterId, @Param("userId") Long userId);
}