package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.entity.DataPool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据池Mapper
 */
@Mapper
public interface DataPoolMapper extends BaseMapper<DataPool> {
    
    /**
     * 全文搜索数据
     */
    @Select("SELECT * FROM data_pool WHERE MATCH(title, content, description) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) AND status = 1 LIMIT #{limit}")
    List<DataPool> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
    
    /**
     * 按分类查询数据
     */
    @Select("SELECT * FROM data_pool WHERE category = #{category} AND status = 1 ORDER BY created_at DESC")
    List<DataPool> selectByCategory(@Param("category") String category);
    
    /**
     * 按类型查询数据
     */
    @Select("SELECT * FROM data_pool WHERE data_type = #{dataType} AND status = 1 ORDER BY created_at DESC")
    List<DataPool> selectByDataType(@Param("dataType") String dataType);
    
    /**
     * 查询公开数据
     */
    @Select("SELECT * FROM data_pool WHERE is_public = 1 AND status = 1 ORDER BY created_at DESC")
    List<DataPool> selectPublicData();
}
