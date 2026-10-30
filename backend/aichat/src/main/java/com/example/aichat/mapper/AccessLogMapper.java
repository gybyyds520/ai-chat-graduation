package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 访问日志Mapper
 */
@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLog> {
    
    /**
     * 查询AI系统的访问日志
     */
    @Select("SELECT * FROM access_log WHERE ai_system_id = #{aiSystemId} ORDER BY accessed_at DESC LIMIT #{limit}")
    List<AccessLog> selectByAiSystemId(@Param("aiSystemId") Long aiSystemId, @Param("limit") int limit);
    
    /**
     * 查询数据的访问日志
     */
    @Select("SELECT * FROM access_log WHERE data_id = #{dataId} ORDER BY accessed_at DESC")
    List<AccessLog> selectByDataId(@Param("dataId") Long dataId);
    
    /**
     * 统计访问类型数量
     */
    @Select("SELECT access_type, COUNT(*) as count FROM access_log WHERE ai_system_id = #{aiSystemId} GROUP BY access_type")
    List<Map<String, Object>> countByAccessType(@Param("aiSystemId") Long aiSystemId);
}
