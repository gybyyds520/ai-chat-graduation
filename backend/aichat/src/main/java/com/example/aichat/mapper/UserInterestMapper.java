package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.UserInterest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserInterestMapper extends BaseMapper<UserInterest> {
    
    @Select("SELECT * FROM user_interest WHERE user_id = #{userId} ORDER BY weight DESC")
    List<UserInterest> getInterestsByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM user_interest WHERE user_id = #{userId} AND interest = #{interest}")
    UserInterest findByUserAndInterest(@Param("userId") Long userId, @Param("interest") String interest);
    
    @Update("UPDATE user_interest SET weight = weight + #{weight} WHERE user_id = #{userId} AND interest = #{interest}")
    int incrementWeight(@Param("userId") Long userId, @Param("interest") String interest, @Param("weight") int weight);
}