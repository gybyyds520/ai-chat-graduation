package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.GroupAiMember;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GroupAiMemberMapper 契约测试（反射校验接口定义，无需真实数据库）。
 *
 * 覆盖：继承 BaseMapper<GroupAiMember>、findActiveByGroupId / findByGroupAndCharacter 方法存在且带 @Select。
 */
class GroupAiMemberMapperTest {

    @Test
    void 应继承BaseMapper且泛型为GroupAiMember() throws Exception {
        Class<?> superInterface = GroupAiMemberMapper.class.getInterfaces()[0];
        assertEquals(BaseMapper.class, superInterface);

        ParameterizedType generic =
                (ParameterizedType) GroupAiMemberMapper.class.getGenericInterfaces()[0];
        assertEquals(GroupAiMember.class, generic.getActualTypeArguments()[0]);
    }

    @Test
    void findActiveByGroupId_应声明且带Select注解() throws Exception {
        Method m = GroupAiMemberMapper.class.getMethod("findActiveByGroupId", Long.class);
        assertEquals(java.util.List.class, m.getReturnType());
        Select select = m.getAnnotation(Select.class);
        assertNotNull(select);
        assertTrue(select.value()[0].contains("is_active"));
    }

    @Test
    void findByGroupAndCharacter_应声明且带Select注解() throws Exception {
        Method m = GroupAiMemberMapper.class.getMethod("findByGroupAndCharacter", Long.class, Long.class);
        assertEquals(GroupAiMember.class, m.getReturnType());
        Select select = m.getAnnotation(Select.class);
        assertNotNull(select);
        assertTrue(select.value()[0].contains("character_id"));
    }
}
