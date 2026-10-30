package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.OAuthProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OAuthProviderMapper extends BaseMapper<OAuthProvider> {

    @Select("SELECT * FROM oauth_provider WHERE provider = #{provider} AND provider_id = #{providerId}")
    OAuthProvider findByProviderAndProviderId(@Param("provider") String provider, @Param("providerId") String providerId);

    @Select("SELECT * FROM oauth_provider WHERE user_id = #{userId} AND provider = #{provider}")
    OAuthProvider findByUserIdAndProvider(@Param("userId") Long userId, @Param("provider") String provider);
}