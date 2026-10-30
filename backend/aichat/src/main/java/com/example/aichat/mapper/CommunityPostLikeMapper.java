package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.CommunityPostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommunityPostLikeMapper extends BaseMapper<CommunityPostLike> {

    @Select("SELECT * FROM community_post_like WHERE post_id = #{postId} AND user_id = #{userId} LIMIT 1")
    CommunityPostLike findByPostAndUser(@Param("postId") Long postId, @Param("userId") Long userId);
}
