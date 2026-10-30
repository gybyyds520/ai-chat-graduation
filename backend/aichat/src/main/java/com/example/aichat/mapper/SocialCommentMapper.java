package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.SocialComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SocialCommentMapper extends BaseMapper<SocialComment> {
    
    @Select("SELECT sc.* FROM social_comment sc WHERE sc.post_id = #{postId} AND sc.parent_id IS NULL AND sc.is_deleted = FALSE ORDER BY sc.created_at DESC")
    List<SocialComment> getCommentsByPost(@Param("postId") Long postId);
    
    @Select("SELECT sc.* FROM social_comment sc WHERE sc.parent_id = #{parentId} AND sc.is_deleted = FALSE ORDER BY sc.created_at DESC")
    List<SocialComment> getRepliesByParent(@Param("parentId") Long parentId);
    
    @Update("UPDATE social_comment SET like_count = like_count + 1 WHERE id = #{commentId}")
    int incrementLikeCount(@Param("commentId") Long commentId);
    
    @Update("UPDATE social_comment SET like_count = like_count - 1 WHERE id = #{commentId} AND like_count > 0")
    int decrementLikeCount(@Param("commentId") Long commentId);
}