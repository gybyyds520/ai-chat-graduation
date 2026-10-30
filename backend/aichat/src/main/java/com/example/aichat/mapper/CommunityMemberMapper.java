package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.CommunityMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommunityMemberMapper extends BaseMapper<CommunityMember> {
    
    @Select("SELECT * FROM community_member WHERE community_id = #{communityId}")
    List<CommunityMember> getMembersByCommunityId(@Param("communityId") Long communityId);
    
    @Select("SELECT * FROM community_member WHERE user_id = #{userId}")
    List<CommunityMember> getCommunitiesByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM community_member WHERE community_id = #{communityId} AND user_id = #{userId}")
    CommunityMember findMember(@Param("communityId") Long communityId, @Param("userId") Long userId);
}