package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.UserStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserStatisticsMapper extends BaseMapper<UserStatistics> {
    
    @Select("SELECT * FROM user_statistics WHERE user_id = #{userId}")
    UserStatistics getByUserId(@Param("userId") Long userId);
    
    @Update("UPDATE user_statistics SET post_count = post_count + 1 WHERE user_id = #{userId}")
    int incrementPostCount(@Param("userId") Long userId);
    
    @Update("UPDATE user_statistics SET comment_count = comment_count + 1 WHERE user_id = #{userId}")
    int incrementCommentCount(@Param("userId") Long userId);
    
    @Update("UPDATE user_statistics SET like_count = like_count + #{count} WHERE user_id = #{userId}")
    int incrementLikeCount(@Param("userId") Long userId, @Param("count") int count);
    
    @Update("UPDATE user_statistics SET follower_count = follower_count + 1 WHERE user_id = #{userId}")
    int incrementFollowerCount(@Param("userId") Long userId);
    
    @Update("UPDATE user_statistics SET following_count = following_count + 1 WHERE user_id = #{userId}")
    int incrementFollowingCount(@Param("userId") Long userId);
    
    @Update("UPDATE user_statistics SET share_count = share_count + 1 WHERE user_id = #{userId}")
    int incrementShareCount(@Param("userId") Long userId);
}