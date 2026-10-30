package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.PayAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PayAccountMapper extends BaseMapper<PayAccount> {

    List<PayAccount> selectByUserId(@Param("userId") Long userId);

    List<PayAccount> selectByUserIdAndType(@Param("userId") Long userId, @Param("accountType") String accountType);

    int countByUserId(@Param("userId") Long userId);
}