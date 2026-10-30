package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.NotificationSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationSettingMapper extends BaseMapper<NotificationSetting> {

    @Select("SELECT * FROM notification_setting WHERE user_id = #{userId} LIMIT 1")
    NotificationSetting selectByUserId(@Param("userId") Long userId);
}
