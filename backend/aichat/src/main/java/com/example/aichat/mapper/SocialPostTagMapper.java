package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.SocialPostTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SocialPostTagMapper extends BaseMapper<SocialPostTag> {
    
    @Select("SELECT * FROM social_post_tag WHERE post_id = #{postId}")
    List<SocialPostTag> getTagsByPostId(@Param("postId") Long postId);
    
    @Select("SELECT DISTINCT tag_name FROM social_post_tag ORDER BY tag_name")
    List<String> getAllTags();
    
    @Select("SELECT DISTINCT tag_name FROM social_post_tag WHERE tag_name LIKE CONCAT('%', #{keyword}, '%') ORDER BY tag_name")
    List<String> searchTags(@Param("keyword") String keyword);
    
    @Select("SELECT post_id FROM social_post_tag WHERE tag_name = #{tagName}")
    List<Long> getPostIdsByTag(@Param("tagName") String tagName);
}