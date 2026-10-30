package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.UserCheckin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface UserCheckinMapper extends BaseMapper<UserCheckin> {

    /** 查询某用户某日的签到记录。 */
    @Select("SELECT * FROM user_checkin WHERE user_id = #{userId} AND checkin_date = #{date} LIMIT 1")
    UserCheckin findByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /** 查询某用户最近一次签到记录（按日期倒序）。 */
    @Select("SELECT * FROM user_checkin WHERE user_id = #{userId} ORDER BY checkin_date DESC LIMIT 1")
    UserCheckin findLatestByUser(@Param("userId") Long userId);
}
