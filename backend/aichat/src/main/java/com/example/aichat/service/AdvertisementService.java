package com.example.aichat.service;

import com.example.aichat.entity.Advertisement;

import java.util.List;

/**
 * 广告服务接口
 */
public interface AdvertisementService {

    /**
     * 创建广告
     *
     * @param ad 广告实体
     * @return 创建的广告
     */
    Advertisement createAd(Advertisement ad);

    /**
     * 更新广告
     *
     * @param ad 广告实体
     * @return 更新后的广告
     */
    Advertisement updateAd(Advertisement ad);

    /**
     * 删除广告
     *
     * @param id 广告ID
     */
    void deleteAd(Long id);

    /**
     * 获取广告详情
     *
     * @param id 广告ID
     * @return 广告实体
     */
    Advertisement getAdById(Long id);

    /**
     * 获取所有广告
     *
     * @return 广告列表
     */
    List<Advertisement> getAllAds();

    /**
     * 根据位置获取广告
     *
     * @param position 广告位置
     * @return 广告列表
     */
    List<Advertisement> getAdsByPosition(String position);

    /**
     * 获取当前生效的广告
     *
     * @param position 广告位置
     * @return 广告列表
     */
    List<Advertisement> getActiveAds(String position);

    /**
     * 启用广告
     *
     * @param id 广告ID
     */
    void enableAd(Long id);

    /**
     * 禁用广告
     *
     * @param id 广告ID
     */
    void disableAd(Long id);

    /**
     * 增加点击次数
     *
     * @param id 广告ID
     */
    void trackClick(Long id);
}