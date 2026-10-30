package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.InterestCommunity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InterestCommunityMapper extends BaseMapper<InterestCommunity> {
    
    @Select("SELECT * FROM interest_community WHERE visibility = 'public' ORDER BY member_count DESC")
    List<InterestCommunity> getPublicCommunities();
    
    @Select("SELECT * FROM interest_community WHERE owner_id = #{userId}")
    List<InterestCommunity> getCommunitiesByOwner(@Param("userId") Long userId);
    
    @Select("SELECT * FROM interest_community WHERE category = #{category} AND visibility = 'public' ORDER BY member_count DESC")
    List<InterestCommunity> getCommunitiesByCategory(@Param("category") String category);
    
    @Select("SELECT * FROM interest_community WHERE name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%') ORDER BY member_count DESC")
    List<InterestCommunity> searchCommunities(@Param("keyword") String keyword);
}