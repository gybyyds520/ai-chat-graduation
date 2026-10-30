package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.SocialLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SocialLikeMapper extends BaseMapper<SocialLike> {
    
    @Select("SELECT COUNT(*) FROM social_like WHERE target_type = #{targetType} AND target_id = #{targetId}")
    int countLikes(@Param("targetType") String targetType, @Param("targetId") Long targetId);
    
    @Select("SELECT * FROM social_like WHERE target_type = #{targetType} AND target_id = #{targetId} AND user_id = #{userId}")
    SocialLike findLike(@Param("targetType") String targetType, @Param("targetId") Long targetId, @Param("userId") Long userId);
    
    @Select("SELECT sl.* FROM social_like sl WHERE sl.target_type = #{targetType} AND sl.target_id = #{targetId} ORDER BY sl.created_at DESC LIMIT #{limit}")
    List<SocialLike> getLikes(@Param("targetType") String targetType, @Param("targetId") Long targetId, @Param("limit") int limit);
}