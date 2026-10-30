package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.Advertisement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 广告Mapper接口
 */
@Mapper
public interface AdvertisementMapper extends BaseMapper<Advertisement> {

    /**
     * 获取当前生效的广告列表
     *
     * @param position 广告位置
     * @param now      当前时间
     * @return 广告列表
     */
    @Select("SELECT * FROM advertisement " +
            "WHERE position = #{position} " +
            "AND status = 'ENABLED' " +
            "AND (start_time IS NULL OR start_time <= #{now}) " +
            "AND (end_time IS NULL OR end_time >= #{now}) " +
            "ORDER BY priority ASC, created_at DESC")
    List<Advertisement> getActiveAdsByPosition(String position, LocalDateTime now);

    /**
     * 根据位置获取所有广告（包含禁用的）
     *
     * @param position 广告位置
     * @return 广告列表
     */
    @Select("SELECT * FROM advertisement WHERE position = #{position} ORDER BY priority ASC")
    List<Advertisement> getAllAdsByPosition(String position);

    /**
     * 增加点击次数
     *
     * @param id 广告ID
     */
    @Select("UPDATE advertisement SET click_count = click_count + 1 WHERE id = #{id}")
    void incrementClickCount(Long id);
}