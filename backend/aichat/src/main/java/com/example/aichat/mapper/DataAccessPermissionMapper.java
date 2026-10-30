package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.DataAccessPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据访问权限Mapper
 */
@Mapper
public interface DataAccessPermissionMapper extends BaseMapper<DataAccessPermission> {
    
    /**
     * 查询AI系统对特定数据的权限
     */
    @Select("SELECT * FROM data_access_permission WHERE ai_system_id = #{aiSystemId} AND data_id = #{dataId} AND (expires_at IS NULL OR expires_at > NOW())")
    DataAccessPermission selectBySystemAndData(@Param("aiSystemId") Long aiSystemId, @Param("dataId") Long dataId);
    
    /**
     * 查询AI系统的所有权限
     */
    @Select("SELECT * FROM data_access_permission WHERE ai_system_id = #{aiSystemId} AND (expires_at IS NULL OR expires_at > NOW())")
    List<DataAccessPermission> selectByAiSystemId(@Param("aiSystemId") Long aiSystemId);
    
    /**
     * 查询数据的所有权限
     */
    @Select("SELECT * FROM data_access_permission WHERE data_id = #{dataId}")
    List<DataAccessPermission> selectByDataId(@Param("dataId") Long dataId);
}
