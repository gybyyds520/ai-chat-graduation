package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.FriendGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendGroupMapper extends BaseMapper<FriendGroup> {
    
    @Select("SELECT fg.* FROM friend_group fg WHERE fg.user_id = #{userId} ORDER BY fg.sort_order ASC")
    List<FriendGroup> getGroupsByUserId(@Param("userId") Long userId);
    
    @Select("SELECT fg.* FROM friend_group fg WHERE fg.user_id = #{userId} AND fg.name = #{name}")
    FriendGroup findByName(@Param("userId") Long userId, @Param("name") String name);
}