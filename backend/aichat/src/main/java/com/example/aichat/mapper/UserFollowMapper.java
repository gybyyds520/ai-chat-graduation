package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {
    
    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id = #{followerId}")
    int countFollowing(@Param("followerId") Long followerId);
    
    @Select("SELECT COUNT(*) FROM user_follow WHERE following_id = #{followingId}")
    int countFollowers(@Param("followingId") Long followingId);
    
    @Select("SELECT uf.* FROM user_follow uf WHERE uf.follower_id = #{followerId} ORDER BY uf.created_at DESC")
    List<UserFollow> getFollowing(@Param("followerId") Long followerId);
    
    @Select("SELECT uf.* FROM user_follow uf WHERE uf.following_id = #{followingId} ORDER BY uf.created_at DESC")
    List<UserFollow> getFollowers(@Param("followingId") Long followingId);
    
    @Select("SELECT * FROM user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    UserFollow findFollow(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
}