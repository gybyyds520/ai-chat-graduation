package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.GroupBot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupBotMapper extends BaseMapper<GroupBot> {
    
    @Select("SELECT * FROM group_bot WHERE group_id = #{groupId} AND enabled = TRUE")
    List<GroupBot> findEnabledBotsByGroupId(@Param("groupId") Long groupId);
}
