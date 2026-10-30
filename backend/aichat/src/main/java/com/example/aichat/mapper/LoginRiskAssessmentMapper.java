package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.LoginRiskAssessment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LoginRiskAssessmentMapper extends BaseMapper<LoginRiskAssessment> {

    @Select("SELECT * FROM login_risk_assessment WHERE user_id = #{userId} ORDER BY login_time DESC LIMIT #{limit}")
    List<LoginRiskAssessment> getAssessmentsByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM login_risk_assessment WHERE user_id = #{userId} AND risk_level = 'HIGH' AND login_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)")
    int countHighRiskLogins24h(@Param("userId") Long userId);
}