package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.DataPoolConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 数据池配置 Mapper。
 */
@Mapper
public interface DataPoolConfigMapper extends BaseMapper<DataPoolConfig> {

    /**
     * 按配置键查询配置项。
     */
    @Select("SELECT * FROM data_pool_config WHERE config_key = #{configKey} LIMIT 1")
    DataPoolConfig selectByKey(@Param("configKey") String configKey);
}
