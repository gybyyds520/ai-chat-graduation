package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.GroupAiMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupAiMemberMapper extends BaseMapper<GroupAiMember> {

    /** 查询某群所有激活的 AI 角色成员（按 id 升序，即添加顺序）。 */
    @Select("SELECT * FROM group_ai_member WHERE group_id = #{groupId} AND is_active = 1 ORDER BY id ASC")
    List<GroupAiMember> findActiveByGroupId(@Param("groupId") Long groupId);

    /** 查询某群指定 AI 角色的成员记录。 */
    @Select("SELECT * FROM group_ai_member WHERE group_id = #{groupId} AND character_id = #{characterId}")
    GroupAiMember findByGroupAndCharacter(@Param("groupId") Long groupId, @Param("characterId") Long characterId);
}
