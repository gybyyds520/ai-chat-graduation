package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.GroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupMessageMapper extends BaseMapper<GroupMessage> {
    
    @Select("SELECT * FROM group_message WHERE group_id = #{groupId} ORDER BY created_at DESC LIMIT #{limit}")
    List<GroupMessage> getMessagesByGroupId(@Param("groupId") Long groupId, @Param("limit") int limit);
}