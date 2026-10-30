package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.HealthAnalysisRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HealthAnalysisRecordMapper extends BaseMapper<HealthAnalysisRecord> {
    
    @Select("SELECT * FROM health_analysis_record WHERE elf_id = #{elfId} AND analysis_date = #{date}")
    HealthAnalysisRecord findByElfIdAndDate(@Param("elfId") Long elfId, @Param("date") LocalDate date);
    
    @Select("SELECT * FROM health_analysis_record WHERE elf_id = #{elfId} ORDER BY analysis_date DESC LIMIT #{limit}")
    List<HealthAnalysisRecord> findByElfIdRecent(@Param("elfId") Long elfId, @Param("limit") int limit);
}
