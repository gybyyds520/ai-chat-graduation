package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.CharacterMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CharacterMemoryMapper extends BaseMapper<CharacterMemory> {
    
    @Select("SELECT * FROM character_memory WHERE character_id = #{characterId} ORDER BY importance DESC LIMIT #{limit}")
    List<CharacterMemory> getImportantMemories(@Param("characterId") Long characterId, @Param("limit") int limit);
    
    @Select("SELECT * FROM character_memory WHERE character_id = #{characterId} AND memory_key = #{memoryKey}")
    CharacterMemory findByKey(@Param("characterId") Long characterId, @Param("memoryKey") String memoryKey);
    
    @Select("SELECT * FROM character_memory WHERE character_id = #{characterId} AND memory_type = #{memoryType} ORDER BY last_accessed DESC")
    List<CharacterMemory> getByType(@Param("characterId") Long characterId, @Param("memoryType") String memoryType);
}