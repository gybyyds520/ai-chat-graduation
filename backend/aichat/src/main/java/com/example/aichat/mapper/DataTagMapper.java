package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.DataTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据标签Mapper
 */
@Mapper
public interface DataTagMapper extends BaseMapper<DataTag> {
    
    @Select("SELECT * FROM data_tag WHERE tag_name = #{tagName}")
    DataTag selectByTagName(String tagName);
    
    @Select("SELECT * FROM data_tag WHERE tag_type = #{tagType}")
    List<DataTag> selectByTagType(@Param("tagType") String tagType);
    
    @Select("SELECT t.* FROM data_tag t INNER JOIN data_tag_relation r ON t.id = r.tag_id WHERE r.data_id = #{dataId}")
    List<DataTag> selectTagsByDataId(@Param("dataId") Long dataId);
}
