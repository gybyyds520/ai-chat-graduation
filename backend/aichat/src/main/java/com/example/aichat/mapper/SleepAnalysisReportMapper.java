package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.SleepAnalysisReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SleepAnalysisReportMapper extends BaseMapper<SleepAnalysisReport> {
    
    @Select("SELECT * FROM sleep_analysis_report WHERE elf_id = #{elfId} ORDER BY report_date DESC LIMIT #{limit}")
    List<SleepAnalysisReport> getReportsByElfId(@Param("elfId") Long elfId, @Param("limit") int limit);
    
    @Select("SELECT * FROM sleep_analysis_report WHERE elf_id = #{elfId} AND report_date = #{date}")
    SleepAnalysisReport getReportByDate(@Param("elfId") Long elfId, @Param("date") LocalDate date);
}