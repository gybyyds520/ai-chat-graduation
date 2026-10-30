package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.SocialPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SocialPostMapper extends BaseMapper<SocialPost> {
    
    @Select("SELECT sp.* FROM social_post sp WHERE sp.is_deleted = FALSE AND sp.visibility = 'public' ORDER BY sp.created_at DESC LIMIT #{limit}")
    List<SocialPost> getPublicPosts(@Param("limit") int limit);
    
    @Select("SELECT sp.* FROM social_post sp WHERE sp.user_id = #{userId} AND sp.is_deleted = FALSE ORDER BY sp.created_at DESC LIMIT #{limit}")
    List<SocialPost> getUserPosts(@Param("userId") Long userId, @Param("limit") int limit);
    
    @Select("SELECT sp.* FROM social_post sp WHERE sp.user_id IN (#{userIds}) AND sp.is_deleted = FALSE ORDER BY sp.created_at DESC LIMIT #{limit}")
    List<SocialPost> getFriendsPosts(@Param("userIds") List<Long> userIds, @Param("limit") int limit);
    
    @Update("UPDATE social_post SET like_count = like_count + 1 WHERE id = #{postId}")
    int incrementLikeCount(@Param("postId") Long postId);
    
    @Update("UPDATE social_post SET like_count = like_count - 1 WHERE id = #{postId} AND like_count > 0")
    int decrementLikeCount(@Param("postId") Long postId);
    
    @Update("UPDATE social_post SET comment_count = comment_count + 1 WHERE id = #{postId}")
    int incrementCommentCount(@Param("postId") Long postId);
    
    @Update("UPDATE social_post SET share_count = share_count + 1 WHERE id = #{postId}")
    int incrementShareCount(@Param("postId") Long postId);
}