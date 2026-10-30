package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.CommunityPostComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommunityPostCommentMapper extends BaseMapper<CommunityPostComment> {

    @Select("SELECT * FROM community_post_comment WHERE post_id = #{postId} ORDER BY created_at DESC")
    List<CommunityPostComment> findByPostId(@Param("postId") Long postId);

    @Select("SELECT COUNT(*) FROM community_post_comment WHERE post_id = #{postId}")
    long countByPostId(@Param("postId") Long postId);
}
