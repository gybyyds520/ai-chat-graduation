package com.example.aichat.service.impl;

import com.example.aichat.entity.Advertisement;
import com.example.aichat.mapper.AdvertisementMapper;
import com.example.aichat.service.AdvertisementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 广告服务实现类
 */
@Service
public class AdvertisementServiceImpl implements AdvertisementService {

    private final AdvertisementMapper advertisementMapper;

    public AdvertisementServiceImpl(AdvertisementMapper advertisementMapper) {
        this.advertisementMapper = advertisementMapper;
    }

    @Override
    @Transactional
    public Advertisement createAd(Advertisement ad) {
        ad.setCreatedAt(LocalDateTime.now());
        ad.setUpdatedAt(LocalDateTime.now());
        ad.setClickCount(0);
        if (ad.getPriority() == null) {
            ad.setPriority(100);
        }
        advertisementMapper.insert(ad);
        return ad;
    }

    @Override
    @Transactional
    public Advertisement updateAd(Advertisement ad) {
        ad.setUpdatedAt(LocalDateTime.now());
        advertisementMapper.updateById(ad);
        return advertisementMapper.selectById(ad.getId());
    }

    @Override
    @Transactional
    public void deleteAd(Long id) {
        advertisementMapper.deleteById(id);
    }

    @Override
    public Advertisement getAdById(Long id) {
        return advertisementMapper.selectById(id);
    }

    @Override
    public List<Advertisement> getAllAds() {
        return advertisementMapper.selectList(null);
    }

    @Override
    public List<Advertisement> getAdsByPosition(String position) {
        return advertisementMapper.getAllAdsByPosition(position);
    }

    @Override
    public List<Advertisement> getActiveAds(String position) {
        return advertisementMapper.getActiveAdsByPosition(position, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void enableAd(Long id) {
        Advertisement ad = advertisementMapper.selectById(id);
        if (ad != null) {
            ad.setStatus("ENABLED");
            ad.setUpdatedAt(LocalDateTime.now());
            advertisementMapper.updateById(ad);
        }
    }

    @Override
    @Transactional
    public void disableAd(Long id) {
        Advertisement ad = advertisementMapper.selectById(id);
        if (ad != null) {
            ad.setStatus("DISABLED");
            ad.setUpdatedAt(LocalDateTime.now());
            advertisementMapper.updateById(ad);
        }
    }

    @Override
    @Transactional
    public void trackClick(Long id) {
        advertisementMapper.incrementClickCount(id);
    }
}