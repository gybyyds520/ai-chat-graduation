package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.SmsCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SmsCodeMapper extends BaseMapper<SmsCode> {
    
    @Select("SELECT * FROM sms_code WHERE phone = #{phone} AND used = false AND expire_time > NOW() ORDER BY created_at DESC LIMIT 1")
    SmsCode getValidCode(@Param("phone") String phone);
    
    @Update("UPDATE sms_code SET used = true WHERE id = #{id}")
    void markAsUsed(@Param("id") Long id);

    /**
     * P1-1：新码下发时使该手机号此前所有未使用的验证码失效，
     * 避免一次登录周期内存在多个有效验证码可被尝试。
     */
    @Update("UPDATE sms_code SET used = true WHERE phone = #{phone} AND used = false")
    void invalidateUnusedCodes(@Param("phone") String phone);
}